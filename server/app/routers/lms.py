import json
from datetime import datetime, timezone
from typing import Dict, List, Optional

from fastapi import APIRouter, Depends, HTTPException, status
from pydantic import BaseModel, Field
from sqlalchemy.orm import Session

from app.core.security import get_optional_user
from app.db.models import Course, CourseLesson, User, UserCoursePurchase, UserLessonProgress
from app.db.seed import ensure_default_user
from app.db.session import get_db

router = APIRouter(prefix="/api/v1/courses", tags=["lms"])


class LessonProgressBody(BaseModel):
    completed: bool = True
    position_sec: int = Field(default=0, ge=0)
    quiz_answers: Optional[Dict[str, int]] = None


def _resolve_user(db: Session, optional_user: Optional[User]) -> User:
    if optional_user is not None:
        return optional_user
    user = ensure_default_user(db)
    db.commit()
    return user


def _has_course_access(db: Session, user_id: int, course_id: int) -> bool:
    return (
        db.query(UserCoursePurchase)
        .filter(UserCoursePurchase.user_id == user_id, UserCoursePurchase.course_id == course_id)
        .first()
        is not None
    )


def _serialize_course_lms(db: Session, course: Course, user_id: int) -> dict:
    payload = json.loads(course.payload_json)
    purchased = _has_course_access(db, user_id, course.id)
    payload["purchased"] = purchased
    if purchased:
        for m in payload.get("modules", []):
            m["locked"] = False

    lessons = (
        db.query(CourseLesson)
        .filter(CourseLesson.course_id == course.id)
        .order_by(CourseLesson.module_number.asc(), CourseLesson.lesson_number.asc())
        .all()
    )
    progress_map = {
        p.lesson_id: p
        for p in db.query(UserLessonProgress).filter(UserLessonProgress.user_id == user_id).all()
    }

    lesson_items: List[dict] = []
    completed_count = 0
    for les in lessons:
        prog = progress_map.get(les.id)
        is_completed = bool(prog and prog.completed)
        if is_completed:
            completed_count += 1
        unlocked = purchased or les.is_preview
        lesson_items.append(
            {
                "id": les.id,
                "course_id": les.course_id,
                "module_number": les.module_number,
                "lesson_number": les.lesson_number,
                "title": les.title,
                "title_uz": les.title_uz,
                "lesson_type": les.lesson_type,
                "duration_min": les.duration_min,
                "is_preview": les.is_preview,
                "locked": not unlocked,
                "completed": is_completed,
                "quiz_score": prog.quiz_score if prog else None,
            }
        )

    total_lessons = len(lesson_items)
    payload["lessons"] = lesson_items
    payload["progress"] = {
        "completed_lessons": completed_count,
        "total_lessons": total_lessons,
        "progress_pct": round((completed_count / total_lessons * 100.0), 1) if total_lessons > 0 else 0.0,
    }
    return payload


@router.get("")
def list_courses_lms(
    optional_user: Optional[User] = Depends(get_optional_user),
    db: Session = Depends(get_db),
):
    user = _resolve_user(db, optional_user)
    rows = db.query(Course).order_by(Course.id.asc()).all()
    return {"courses": [_serialize_course_lms(db, c, user.id) for c in rows]}


@router.get("/{course_id}")
def get_course_lms(
    course_id: int,
    optional_user: Optional[User] = Depends(get_optional_user),
    db: Session = Depends(get_db),
):
    user = _resolve_user(db, optional_user)
    course = db.get(Course, course_id)
    if not course:
        raise HTTPException(status_code=404, detail="Course not found")
    return _serialize_course_lms(db, course, user.id)


@router.get("/{course_id}/lessons/{lesson_id}")
def get_lesson_detail(
    course_id: int,
    lesson_id: int,
    optional_user: Optional[User] = Depends(get_optional_user),
    db: Session = Depends(get_db),
):
    user = _resolve_user(db, optional_user)
    lesson = (
        db.query(CourseLesson)
        .filter(CourseLesson.id == lesson_id, CourseLesson.course_id == course_id)
        .first()
    )
    if not lesson:
        raise HTTPException(status_code=404, detail="Lesson not found")

    purchased = _has_course_access(db, user.id, course_id)
    if not lesson.is_preview and not purchased:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Course purchase required to access this lesson",
        )

    prog = (
        db.query(UserLessonProgress)
        .filter(UserLessonProgress.user_id == user.id, UserLessonProgress.lesson_id == lesson.id)
        .first()
    )
    quiz_raw = json.loads(lesson.quiz_json or "[]")
    # Strip correct_index before sending questions to client
    quiz_public = [
        {
            "id": q["id"],
            "question": q["question"],
            "question_uz": q.get("question_uz", q["question"]),
            "options": q["options"],
        }
        for q in quiz_raw
    ]
    return {
        "id": lesson.id,
        "course_id": lesson.course_id,
        "module_number": lesson.module_number,
        "lesson_number": lesson.lesson_number,
        "title": lesson.title,
        "title_uz": lesson.title_uz,
        "lesson_type": lesson.lesson_type,
        "duration_min": lesson.duration_min,
        "video_url": lesson.video_url,
        "content_md": lesson.content_md,
        "content_md_uz": lesson.content_md_uz,
        "is_preview": lesson.is_preview,
        "quiz": quiz_public,
        "completed": bool(prog and prog.completed),
        "position_sec": prog.position_sec if prog else 0,
        "quiz_score": prog.quiz_score if prog else None,
    }


@router.post("/{course_id}/lessons/{lesson_id}/progress")
def update_lesson_progress(
    course_id: int,
    lesson_id: int,
    body: LessonProgressBody,
    optional_user: Optional[User] = Depends(get_optional_user),
    db: Session = Depends(get_db),
):
    user = _resolve_user(db, optional_user)
    lesson = (
        db.query(CourseLesson)
        .filter(CourseLesson.id == lesson_id, CourseLesson.course_id == course_id)
        .first()
    )
    if not lesson:
        raise HTTPException(status_code=404, detail="Lesson not found")

    purchased = _has_course_access(db, user.id, course_id)
    if not lesson.is_preview and not purchased:
        raise HTTPException(status_code=403, detail="Course purchase required")

    quiz_score = None
    if body.quiz_answers is not None:
        quiz_items = json.loads(lesson.quiz_json or "[]")
        if quiz_items:
            correct = sum(
                1
                for q in quiz_items
                if body.quiz_answers.get(q["id"]) == q.get("correct_index")
            )
            quiz_score = round(correct / len(quiz_items) * 100)

    prog = (
        db.query(UserLessonProgress)
        .filter(UserLessonProgress.user_id == user.id, UserLessonProgress.lesson_id == lesson.id)
        .first()
    )
    if not prog:
        prog = UserLessonProgress(user_id=user.id, lesson_id=lesson.id)
    prog.completed = body.completed
    prog.position_sec = body.position_sec
    if quiz_score is not None:
        prog.quiz_score = quiz_score
    prog.updated_at = datetime.now(timezone.utc)
    db.add(prog)
    db.commit()

    course = db.get(Course, course_id)
    return {
        "status": "ok",
        "lesson_id": lesson.id,
        "completed": prog.completed,
        "quiz_score": prog.quiz_score,
        "course_progress": _serialize_course_lms(db, course, user.id)["progress"] if course else None,
    }

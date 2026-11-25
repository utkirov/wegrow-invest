import json
from datetime import datetime, timezone
from typing import Optional

from fastapi import APIRouter, Depends, HTTPException, Query
from pydantic import BaseModel
from sqlalchemy import desc
from sqlalchemy.orm import Session

from app.core.config import get_settings
from app.core.security import get_current_user, get_optional_user
from app.db.models import Course, Goal, Stock, User, UserCoursePurchase
from app.db.seed import ensure_default_user
from app.db.session import get_db
from app.services.market_data import build_market_overview, stock_to_dict
from app.services.stock_universe import INITIAL_NEWS

router = APIRouter(tags=["core"])


class ProfileUpdate(BaseModel):
    first_name: str
    last_name: str
    middle_name: Optional[str] = ""
    birth_date: Optional[str] = "15.04.1996"
    gender: Optional[str] = "male"
    language: Optional[str] = "ru"
    dark_theme: Optional[bool] = True


class GoalCreate(BaseModel):
    title: str
    target_amount: float
    current_amount: float = 0.0
    monthly_contribution: float = 200.0
    deadline_label: str = "Декабрь 2027"
    category: str = "savings"
    icon: str = "target"


class GoalContribute(BaseModel):
    amount: float


class CoursePurchaseRequest(BaseModel):
    provider: str = "Payme"


def _resolve_user(db: Session, optional_user: Optional[User]) -> User:
    if optional_user is not None:
        return optional_user
    user = ensure_default_user(db)
    db.commit()
    return user


def _build_courses_for_user(db: Session, user_id: int) -> list[dict]:
    purchased_map = {
        p.course_id: p.provider
        for p in db.query(UserCoursePurchase).filter(UserCoursePurchase.user_id == user_id).all()
    }
    courses = []
    for row in db.query(Course).order_by(Course.id.asc()).all():
        payload = json.loads(row.payload_json)
        if row.id in purchased_map:
            payload["purchased"] = True
            payload["purchased_via"] = purchased_map[row.id]
            for mod in payload.get("modules", []):
                mod["locked"] = False
        courses.append(payload)
    return courses


def _build_bootstrap(db: Session, user: User, stock_limit: int = 105) -> dict:
    stocks_rows = db.query(Stock).order_by(desc(Stock.market_cap_usd)).limit(stock_limit).all()
    stocks = [stock_to_dict(s) for s in stocks_rows]
    goals_rows = db.query(Goal).filter(Goal.user_id == user.id).order_by(Goal.id.asc()).all()
    goals = [g.to_dict() for g in goals_rows]
    courses = _build_courses_for_user(db, user.id)

    total_saved = sum(g["current_amount"] for g in goals)
    total_target = sum(g["target_amount"] for g in goals)

    return {
        "profile": user.to_profile_dict(),
        "goals_summary": {
            "total_saved": total_saved,
            "total_target": total_target,
            "progress_pct": round((total_saved / total_target * 100.0) if total_target > 0 else 0.0, 1),
            "monthly_delta_pct": 12.8,
            "monthly_delta_usd": 350.0,
            "ahead_label": "Опережаете план на 2 недели",
            "ahead_label_uz": "Rejadan 2 hafta oldindasiz",
        },
        "market_overview": build_market_overview(db),
        "stocks": stocks,
        "goals": goals,
        "courses": courses,
        "news": INITIAL_NEWS,
    }


@router.get("/api/health")
@router.get("/api/v1/health")
def health(db: Session = Depends(get_db)):
    settings = get_settings()
    stock_count = db.query(Stock).count()
    return {
        "status": "ok",
        "service": settings.app_name,
        "version": settings.app_version,
        "stocks_indexed": stock_count,
        "timestamp": datetime.now(timezone.utc).isoformat(),
    }


@router.get("/api/bootstrap")
def bootstrap_legacy(
    optional_user: Optional[User] = Depends(get_optional_user),
    db: Session = Depends(get_db),
):
    user = _resolve_user(db, optional_user)
    return _build_bootstrap(db, user)


@router.get("/api/v1/bootstrap")
def bootstrap_v1(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    return _build_bootstrap(db, current_user)


@router.get("/api/stocks")
def list_stocks_legacy(
    q: Optional[str] = Query(default=None),
    halal_only: bool = Query(default=False),
    sector: Optional[str] = Query(default=None),
    db: Session = Depends(get_db),
):
    rows = db.query(Stock).order_by(desc(Stock.market_cap_usd)).all()
    stocks = [stock_to_dict(r) for r in rows]
    if halal_only:
        stocks = [s for s in stocks if s["halal_status"] == "HALAL"]
    if sector and sector not in ("Все", "Barchasi"):
        stocks = [
            s
            for s in stocks
            if s["sector"].lower() == sector.lower() or s.get("sector_uz", "").lower() == sector.lower()
        ]
    if q:
        ql = q.strip().lower()
        stocks = [
            s
            for s in stocks
            if ql in s["symbol"].lower() or ql in s["name"].lower() or ql in s["sector"].lower()
        ]
    return {"stocks": stocks}


@router.get("/api/stocks/{symbol}")
def get_stock_legacy(symbol: str, db: Session = Depends(get_db)):
    stock = db.query(Stock).filter(Stock.symbol == symbol.strip().upper()).first()
    if not stock:
        raise HTTPException(status_code=404, detail="Stock not found")
    return stock_to_dict(stock)


@router.post("/api/profile")
def update_profile_legacy(
    body: ProfileUpdate,
    optional_user: Optional[User] = Depends(get_optional_user),
    db: Session = Depends(get_db),
):
    user = _resolve_user(db, optional_user)
    user.first_name = body.first_name
    user.last_name = body.last_name
    user.middle_name = body.middle_name or ""
    user.birth_date = body.birth_date or user.birth_date
    user.gender = body.gender or user.gender
    user.language = body.language or user.language
    user.dark_theme = bool(body.dark_theme)
    db.add(user)
    db.commit()
    db.refresh(user)
    return {"status": "ok", "profile": user.to_profile_dict()}


@router.get("/api/goals")
def list_goals_legacy(
    optional_user: Optional[User] = Depends(get_optional_user),
    db: Session = Depends(get_db),
):
    user = _resolve_user(db, optional_user)
    rows = db.query(Goal).filter(Goal.user_id == user.id).order_by(Goal.id.asc()).all()
    return {"goals": [r.to_dict() for r in rows]}


@router.get("/api/v1/goals")
def list_goals_v1(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    rows = db.query(Goal).filter(Goal.user_id == current_user.id).order_by(Goal.id.asc()).all()
    return {"goals": [r.to_dict() for r in rows]}


@router.post("/api/goals")
def create_goal_legacy(
    body: GoalCreate,
    optional_user: Optional[User] = Depends(get_optional_user),
    db: Session = Depends(get_db),
):
    user = _resolve_user(db, optional_user)
    return _create_goal_for_user(db, user.id, body)


@router.post("/api/v1/goals")
def create_goal_v1(
    body: GoalCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    return _create_goal_for_user(db, current_user.id, body)


def _create_goal_for_user(db: Session, user_id: int, body: GoalCreate) -> dict:
    goal = Goal(
        user_id=user_id,
        title=body.title,
        title_uz=body.title,
        target_amount=body.target_amount,
        current_amount=body.current_amount,
        monthly_contribution=body.monthly_contribution,
        deadline_label=body.deadline_label,
        deadline_label_uz=body.deadline_label,
        category=body.category,
        icon=body.icon,
    )
    db.add(goal)
    db.commit()
    db.refresh(goal)
    return {"status": "ok", "goal": goal.to_dict()}


@router.post("/api/goals/{goal_id}/contribute")
def contribute_goal_legacy(
    goal_id: int,
    body: GoalContribute,
    optional_user: Optional[User] = Depends(get_optional_user),
    db: Session = Depends(get_db),
):
    user = _resolve_user(db, optional_user)
    return _contribute_goal_for_user(db, user.id, goal_id, body.amount)


@router.post("/api/v1/goals/{goal_id}/contribute")
def contribute_goal_v1(
    goal_id: int,
    body: GoalContribute,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    return _contribute_goal_for_user(db, current_user.id, goal_id, body.amount)


def _contribute_goal_for_user(db: Session, user_id: int, goal_id: int, amount: float) -> dict:
    goal = db.query(Goal).filter(Goal.id == goal_id, Goal.user_id == user_id).first()
    if not goal:
        raise HTTPException(status_code=404, detail="Goal not found")
    goal.current_amount = min(goal.target_amount, max(0.0, goal.current_amount + amount))
    db.add(goal)
    db.commit()
    db.refresh(goal)
    return {"status": "ok", "goal": goal.to_dict()}


@router.delete("/api/goals/{goal_id}")
def delete_goal_legacy(
    goal_id: int,
    optional_user: Optional[User] = Depends(get_optional_user),
    db: Session = Depends(get_db),
):
    user = _resolve_user(db, optional_user)
    db.query(Goal).filter(Goal.id == goal_id, Goal.user_id == user.id).delete()
    db.commit()
    return {"status": "ok"}


@router.delete("/api/v1/goals/{goal_id}")
def delete_goal_v1(
    goal_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    deleted = db.query(Goal).filter(Goal.id == goal_id, Goal.user_id == current_user.id).delete()
    if not deleted:
        raise HTTPException(status_code=404, detail="Goal not found")
    db.commit()
    return {"status": "ok"}


@router.post("/api/courses/{course_id}/purchase")
def purchase_course_legacy(
    course_id: int,
    body: CoursePurchaseRequest,
    optional_user: Optional[User] = Depends(get_optional_user),
    db: Session = Depends(get_db),
):
    user = _resolve_user(db, optional_user)
    return _purchase_course_for_user(db, user.id, course_id, body.provider)


@router.post("/api/v1/courses/{course_id}/purchase")
def purchase_course_v1(
    course_id: int,
    body: CoursePurchaseRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    return _purchase_course_for_user(db, current_user.id, course_id, body.provider)


def _purchase_course_for_user(db: Session, user_id: int, course_id: int, provider: str) -> dict:
    course = db.get(Course, course_id)
    if not course:
        raise HTTPException(status_code=404, detail="Course not found")

    existing = (
        db.query(UserCoursePurchase)
        .filter(UserCoursePurchase.user_id == user_id, UserCoursePurchase.course_id == course_id)
        .first()
    )
    if not existing:
        db.add(UserCoursePurchase(user_id=user_id, course_id=course_id, provider=provider))
        db.commit()

    payload = json.loads(course.payload_json)
    payload["purchased"] = True
    payload["purchased_via"] = provider
    for mod in payload.get("modules", []):
        mod["locked"] = False
    return {"status": "ok", "course": payload}

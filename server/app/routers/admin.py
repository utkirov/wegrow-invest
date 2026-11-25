import json
import os
import secrets
from datetime import datetime, timezone
from typing import Optional

from fastapi import APIRouter, Depends, Header, HTTPException, Query, status
from fastapi.responses import HTMLResponse
from pydantic import BaseModel, Field
from sqlalchemy import desc
from sqlalchemy.orm import Session

from app.core.config import get_settings
from app.db.models import (
    CourseLesson,
    PaymentOrder,
    PriceAlert,
    Stock,
    User,
)
from app.db.session import get_db
from app.services.halal_screener import AaoifiScreeningInput, screen_stock_aaoifi
from app.services.market_data import build_revenue_segments, stock_to_dict

router = APIRouter(tags=["admin"])

STATIC_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "static")


def verify_admin_key(x_admin_key: Optional[str] = Header(default=None, alias="X-Admin-Key")) -> None:
    expected = get_settings().admin_api_key
    if not x_admin_key or not secrets.compare_digest(x_admin_key, expected):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or missing X-Admin-Key header",
        )


class HalalAuditUpdate(BaseModel):
    debt_ratio_pct: float = Field(..., ge=0.0, le=500.0)
    interest_deposit_ratio_pct: float = Field(..., ge=0.0, le=500.0)
    impure_revenue_pct: float = Field(..., ge=0.0, le=100.0)
    business_override: Optional[str] = Field(default=None, pattern="^(HALAL|DOUBTFUL|HARAM)?$")
    verdict_note: Optional[str] = Field(default=None, max_length=255)


class AdminLessonCreate(BaseModel):
    course_id: int = Field(default=1, ge=1)
    module_number: int = Field(..., ge=1, le=20)
    lesson_number: int = Field(..., ge=1, le=50)
    title: str = Field(..., min_length=3, max_length=255)
    title_uz: str = Field(..., min_length=3, max_length=255)
    lesson_type: str = Field(default="article", pattern="^(video|article|quiz)$")
    duration_min: int = Field(default=10, ge=1, le=240)
    video_url: Optional[str] = None
    content_md: str = Field(default="")
    content_md_uz: str = Field(default="")
    is_preview: bool = False


@router.get("/admin", response_class=HTMLResponse, include_in_schema=False)
def serve_admin_ui():
    html_path = os.path.join(STATIC_DIR, "admin.html")
    with open(html_path, "r", encoding="utf-8") as fh:
        html = fh.read()
    settings = get_settings()
    # Inject default dev key only outside production for instant local admin UX
    default_key = settings.admin_api_key if settings.environment != "production" else ""
    return HTMLResponse(content=html.replace("__DEFAULT_ADMIN_KEY__", default_key))


@router.get("/api/v1/admin/stats", dependencies=[Depends(verify_admin_key)])
def admin_stats(db: Session = Depends(get_db)):
    stocks = db.query(Stock).all()
    halal = sum(1 for s in stocks if s.halal_status == "HALAL")
    doubtful = sum(1 for s in stocks if s.halal_status == "DOUBTFUL")
    haram = sum(1 for s in stocks if s.halal_status == "HARAM")

    orders = db.query(PaymentOrder).all()
    paid_orders = [o for o in orders if o.status == "PAID"]
    revenue_uzs = sum(o.amount_uzs for o in paid_orders)

    return {
        "users_total": db.query(User).count(),
        "stocks_total": len(stocks),
        "stocks_halal": halal,
        "stocks_doubtful": doubtful,
        "stocks_haram": haram,
        "lessons_total": db.query(CourseLesson).count(),
        "orders_total": len(orders),
        "orders_paid": len(paid_orders),
        "revenue_uzs": revenue_uzs,
        "active_alerts": db.query(PriceAlert).filter(PriceAlert.active.is_(True)).count(),
    }


@router.get("/api/v1/admin/stocks", dependencies=[Depends(verify_admin_key)])
def admin_list_stocks(
    q: Optional[str] = Query(default=None),
    halal_status: Optional[str] = Query(default=None),
    db: Session = Depends(get_db),
):
    query = db.query(Stock)
    if halal_status and halal_status.upper() in {"HALAL", "DOUBTFUL", "HARAM"}:
        query = query.filter(Stock.halal_status == halal_status.upper())
    if q and q.strip():
        p = f"%{q.strip()}%"
        query = query.filter(Stock.symbol.ilike(p) | Stock.name.ilike(p) | Stock.sector.ilike(p))
    rows = query.order_by(desc(Stock.market_cap_usd)).all()
    return {
        "stocks": [
            {
                "symbol": s.symbol,
                "name": s.name,
                "sector": s.sector,
                "industry": s.industry,
                "price": round(s.price, 2),
                "market_cap": s.market_cap_label,
                "halal_status": s.halal_status,
                "business_status": s.business_status,
                "debt_ratio_pct": s.debt_ratio_pct,
                "interest_deposit_ratio_pct": s.interest_deposit_ratio_pct,
                "impure_revenue_pct": s.impure_revenue_pct,
                "purification_per_dividend": s.purification_per_dividend,
                "verdict_summary": s.verdict_summary,
                "manual_override": s.manual_override,
                "checked_date": s.checked_date,
            }
            for s in rows
        ]
    }


@router.put("/api/v1/admin/stocks/{symbol}/halal", dependencies=[Depends(verify_admin_key)])
def admin_update_halal_audit(
    symbol: str,
    body: HalalAuditUpdate,
    db: Session = Depends(get_db),
):
    stock = db.query(Stock).filter(Stock.symbol == symbol.strip().upper()).first()
    if not stock:
        raise HTTPException(status_code=404, detail="Stock not found")

    override = body.business_override if body.business_override in {"HALAL", "DOUBTFUL", "HARAM"} else None
    result = screen_stock_aaoifi(
        AaoifiScreeningInput(
            industry=stock.industry,
            debt_ratio_pct=body.debt_ratio_pct,
            interest_deposit_ratio_pct=body.interest_deposit_ratio_pct,
            impure_revenue_pct=body.impure_revenue_pct,
            dividend_per_quarter=stock.dividend_per_quarter,
            business_override=override,
        )
    )

    now = datetime.now(timezone.utc)
    stock.debt_ratio_pct = body.debt_ratio_pct
    stock.interest_deposit_ratio_pct = body.interest_deposit_ratio_pct
    stock.impure_revenue_pct = body.impure_revenue_pct
    stock.business_status = result.business_status
    stock.halal_status = result.halal_status
    stock.purification_per_dividend = result.purification_per_dividend
    stock.verdict_summary = body.verdict_note.strip() if body.verdict_note else result.verdict_summary
    stock.criteria_json = json.dumps(result.criteria, ensure_ascii=False)
    stock.revenue_segments_json = json.dumps(
        build_revenue_segments(stock.sector, body.impure_revenue_pct, result.halal_status),
        ensure_ascii=False,
    )
    stock.manual_override = True
    stock.checked_date = now.strftime("%d.%m.%Y")
    stock.updated_at = now
    db.add(stock)
    db.commit()
    db.refresh(stock)
    return {"status": "ok", "stock": stock_to_dict(stock)}


@router.get("/api/v1/admin/lessons", dependencies=[Depends(verify_admin_key)])
def admin_list_lessons(course_id: int = Query(default=1), db: Session = Depends(get_db)):
    rows = (
        db.query(CourseLesson)
        .filter(CourseLesson.course_id == course_id)
        .order_by(CourseLesson.module_number.asc(), CourseLesson.lesson_number.asc())
        .all()
    )
    return {
        "lessons": [
            {
                "id": l.id,
                "course_id": l.course_id,
                "module_number": l.module_number,
                "lesson_number": l.lesson_number,
                "title": l.title,
                "title_uz": l.title_uz,
                "lesson_type": l.lesson_type,
                "duration_min": l.duration_min,
                "is_preview": l.is_preview,
            }
            for l in rows
        ]
    }


@router.post("/api/v1/admin/lessons", dependencies=[Depends(verify_admin_key)])
def admin_create_lesson(body: AdminLessonCreate, db: Session = Depends(get_db)):
    lesson = CourseLesson(
        course_id=body.course_id,
        module_number=body.module_number,
        lesson_number=body.lesson_number,
        title=body.title,
        title_uz=body.title_uz,
        lesson_type=body.lesson_type,
        duration_min=body.duration_min,
        video_url=body.video_url,
        content_md=body.content_md,
        content_md_uz=body.content_md_uz or body.content_md,
        quiz_json="[]",
        is_preview=body.is_preview,
    )
    db.add(lesson)
    db.commit()
    db.refresh(lesson)
    return {"status": "ok", "lesson_id": lesson.id}


@router.delete("/api/v1/admin/lessons/{lesson_id}", dependencies=[Depends(verify_admin_key)])
def admin_delete_lesson(lesson_id: int, db: Session = Depends(get_db)):
    deleted = db.query(CourseLesson).filter(CourseLesson.id == lesson_id).delete()
    if not deleted:
        raise HTTPException(status_code=404, detail="Lesson not found")
    db.commit()
    return {"status": "ok"}


@router.get("/api/v1/admin/orders", dependencies=[Depends(verify_admin_key)])
def admin_list_orders(db: Session = Depends(get_db)):
    rows = db.query(PaymentOrder).order_by(desc(PaymentOrder.created_at)).limit(50).all()
    return {
        "orders": [
            {
                "order_uid": o.order_uid,
                "user_id": o.user_id,
                "course_id": o.course_id,
                "provider": o.provider,
                "amount_uzs": o.amount_uzs,
                "status": o.status,
                "provider_tx_id": o.provider_tx_id,
                "created_at": o.created_at.isoformat() if o.created_at else None,
                "paid_at": o.paid_at.isoformat() if o.paid_at else None,
            }
            for o in rows
        ]
    }

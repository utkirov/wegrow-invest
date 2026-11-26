import json
import uuid
from datetime import datetime, timezone
from typing import Any, Dict, Optional

from fastapi import APIRouter, Depends, Header, HTTPException, Request
from pydantic import BaseModel, Field
from sqlalchemy.orm import Session

from app.core.config import get_settings
from app.core.security import get_optional_user
from app.db.models import Course, PaymentOrder, User
from app.db.seed import ensure_default_user
from app.db.session import get_db
from app.payments.click import build_click_checkout_url, handle_click_complete, handle_click_prepare
from app.payments.payme import build_payme_checkout_url, handle_payme_rpc, unlock_course_for_order

router = APIRouter(prefix="/api/v1", tags=["payments"])


class CreateOrderBody(BaseModel):
    course_id: int = Field(..., ge=1)
    provider: str = Field(default="PAYME", pattern="^(PAYME|CLICK|Payme|Click|payme|click)$")


def _resolve_user(db: Session, optional_user: Optional[User]) -> User:
    if optional_user is not None:
        return optional_user
    user = ensure_default_user(db)
    db.commit()
    return user


def _order_to_dict(order: PaymentOrder, course_titles: Optional[Dict[int, tuple[str, str]]] = None) -> dict:
    checkout_url = (
        build_payme_checkout_url(order) if order.provider == "PAYME" else build_click_checkout_url(order)
    )
    title_ru, title_uz = (course_titles or {}).get(
        order.course_id,
        ("Основы инвестирования в акции США", "AQSh aksiyalariga investitsiya asoslari"),
    )
    return {
        "order_uid": order.order_uid,
        "course_id": order.course_id,
        "course_title": title_ru,
        "course_title_uz": title_uz,
        "provider": order.provider,
        "amount_uzs": order.amount_uzs,
        "amount_tiyin": order.amount_tiyin,
        "status": order.status,
        "checkout_url": checkout_url,
        "sandbox_enabled": get_settings().payment_sandbox,
        "created_at": order.created_at.isoformat() if order.created_at else None,
        "paid_at": order.paid_at.isoformat() if order.paid_at else None,
    }


def _load_course_titles(db: Session) -> Dict[int, tuple[str, str]]:
    result: Dict[int, tuple[str, str]] = {}
    for row in db.query(Course).all():
        try:
            p = json.loads(row.payload_json)
            result[row.id] = (p.get("title", f"Курс #{row.id}"), p.get("title_uz", p.get("title", f"Kurs #{row.id}")))
        except Exception:
            result[row.id] = (f"Курс #{row.id}", f"Kurs #{row.id}")
    return result


@router.get("/orders")
def list_user_orders(
    optional_user: Optional[User] = Depends(get_optional_user),
    db: Session = Depends(get_db),
):
    user = _resolve_user(db, optional_user)
    rows = (
        db.query(PaymentOrder)
        .filter(PaymentOrder.user_id == user.id)
        .order_by(PaymentOrder.created_at.desc())
        .limit(50)
        .all()
    )
    titles = _load_course_titles(db)
    return {"orders": [_order_to_dict(o, titles) for o in rows]}


@router.post("/orders")
def create_order(
    body: CreateOrderBody,
    optional_user: Optional[User] = Depends(get_optional_user),
    db: Session = Depends(get_db),
):
    user = _resolve_user(db, optional_user)
    course = db.get(Course, body.course_id)
    if not course:
        raise HTTPException(status_code=404, detail="Course not found")

    payload = json.loads(course.payload_json)
    amount_uzs = int(payload.get("price_uzs_num", 1290000))
    provider = body.provider.upper()
    order = PaymentOrder(
        order_uid=f"WG-{uuid.uuid4().hex[:12].upper()}",
        user_id=user.id,
        course_id=course.id,
        provider=provider,
        amount_uzs=amount_uzs,
        amount_tiyin=amount_uzs * 100,
        status="PENDING",
    )
    db.add(order)
    db.commit()
    db.refresh(order)
    titles = {course.id: (payload.get("title", ""), payload.get("title_uz", payload.get("title", "")))}
    return {"status": "ok", "order": _order_to_dict(order, titles)}


@router.get("/orders/{order_uid}")
def get_order_status(
    order_uid: str,
    optional_user: Optional[User] = Depends(get_optional_user),
    db: Session = Depends(get_db),
):
    user = _resolve_user(db, optional_user)
    order = (
        db.query(PaymentOrder)
        .filter(PaymentOrder.order_uid == order_uid, PaymentOrder.user_id == user.id)
        .first()
    )
    if not order:
        raise HTTPException(status_code=404, detail="Order not found")
    return {"order": _order_to_dict(order)}


@router.post("/orders/{order_uid}/sandbox-pay")
def sandbox_pay_order(
    order_uid: str,
    optional_user: Optional[User] = Depends(get_optional_user),
    db: Session = Depends(get_db),
):
    if not get_settings().payment_sandbox:
        raise HTTPException(status_code=403, detail="Sandbox payments disabled in production")
    user = _resolve_user(db, optional_user)
    order = (
        db.query(PaymentOrder)
        .filter(PaymentOrder.order_uid == order_uid, PaymentOrder.user_id == user.id)
        .first()
    )
    if not order:
        raise HTTPException(status_code=404, detail="Order not found")

    order.status = "PAID"
    order.provider_state = 2
    order.paid_at = datetime.now(timezone.utc)
    unlock_course_for_order(db, order)
    db.add(order)
    db.commit()
    db.refresh(order)
    return {"status": "ok", "order": _order_to_dict(order)}


@router.post("/payments/payme/rpc")
async def payme_webhook(
    request: Request,
    authorization: Optional[str] = Header(default=None),
    db: Session = Depends(get_db),
):
    body: Dict[str, Any] = await request.json()
    return handle_payme_rpc(db, body, authorization)


@router.post("/payments/click/prepare")
async def click_prepare_webhook(request: Request, db: Session = Depends(get_db)):
    content_type = request.headers.get("content-type", "")
    if "application/json" in content_type:
        payload = await request.json()
    else:
        form = await request.form()
        payload = dict(form)
    return handle_click_prepare(db, payload)


@router.post("/payments/click/complete")
async def click_complete_webhook(request: Request, db: Session = Depends(get_db)):
    content_type = request.headers.get("content-type", "")
    if "application/json" in content_type:
        payload = await request.json()
    else:
        form = await request.form()
        payload = dict(form)
    return handle_click_complete(db, payload)

import hashlib
from datetime import datetime, timezone
from typing import Optional
from urllib.parse import urlencode

from sqlalchemy.orm import Session

from app.core.config import get_settings
from app.db.models import PaymentOrder
from app.payments.payme import unlock_course_for_order


def build_click_checkout_url(order: PaymentOrder) -> str:
    settings = get_settings()
    params = urlencode(
        {
            "service_id": settings.click_service_id,
            "merchant_id": settings.click_merchant_id,
            "amount": order.amount_uzs,
            "transaction_param": order.order_uid,
        }
    )
    return f"{settings.click_checkout_url.rstrip('/')}?{params}"


def compute_click_signature(
    click_trans_id: str,
    service_id: str,
    merchant_trans_id: str,
    amount: str,
    action: str,
    sign_time: str,
    merchant_prepare_id: Optional[str] = None,
) -> str:
    secret = get_settings().click_secret_key
    if str(action) == "1" and merchant_prepare_id is not None:
        raw = f"{click_trans_id}{service_id}{secret}{merchant_trans_id}{merchant_prepare_id}{amount}{action}{sign_time}"
    else:
        raw = f"{click_trans_id}{service_id}{secret}{merchant_trans_id}{amount}{action}{sign_time}"
    return hashlib.md5(raw.encode("utf-8")).hexdigest()


def handle_click_prepare(db: Session, payload: dict) -> dict:
    click_trans_id = str(payload.get("click_trans_id", ""))
    service_id = str(payload.get("service_id", ""))
    merchant_trans_id = str(payload.get("merchant_trans_id", ""))
    amount = str(payload.get("amount", "0"))
    action = str(payload.get("action", "0"))
    sign_time = str(payload.get("sign_time", ""))
    sign_string = str(payload.get("sign_string", ""))

    expected = compute_click_signature(
        click_trans_id, service_id, merchant_trans_id, amount, action, sign_time
    )
    if sign_string.lower() != expected.lower():
        return {"error": -1, "error_note": "SIGN CHECK FAILED!"}

    order = db.query(PaymentOrder).filter(PaymentOrder.order_uid == merchant_trans_id).first()
    if not order:
        return {"error": -5, "error_note": "Order does not exist"}
    if order.status == "PAID":
        return {"error": -4, "error_note": "Already paid"}
    if order.status == "CANCELLED":
        return {"error": -9, "error_note": "Transaction cancelled"}
    if abs(float(amount) - float(order.amount_uzs)) > 0.01:
        return {"error": -2, "error_note": "Incorrect parameter amount"}

    order.provider_tx_id = click_trans_id
    order.provider_state = 1
    db.add(order)
    db.commit()

    return {
        "click_trans_id": int(click_trans_id) if click_trans_id.isdigit() else click_trans_id,
        "merchant_trans_id": order.order_uid,
        "merchant_prepare_id": order.id,
        "error": 0,
        "error_note": "Success",
    }


def handle_click_complete(db: Session, payload: dict) -> dict:
    click_trans_id = str(payload.get("click_trans_id", ""))
    service_id = str(payload.get("service_id", ""))
    merchant_trans_id = str(payload.get("merchant_trans_id", ""))
    merchant_prepare_id = str(payload.get("merchant_prepare_id", ""))
    amount = str(payload.get("amount", "0"))
    action = str(payload.get("action", "1"))
    sign_time = str(payload.get("sign_time", ""))
    sign_string = str(payload.get("sign_string", ""))
    click_error = int(payload.get("error", 0))

    expected = compute_click_signature(
        click_trans_id,
        service_id,
        merchant_trans_id,
        amount,
        action,
        sign_time,
        merchant_prepare_id=merchant_prepare_id,
    )
    if sign_string.lower() != expected.lower():
        return {"error": -1, "error_note": "SIGN CHECK FAILED!"}

    order = db.query(PaymentOrder).filter(PaymentOrder.order_uid == merchant_trans_id).first()
    if not order or str(order.id) != merchant_prepare_id:
        return {"error": -6, "error_note": "Transaction does not exist"}
    if order.status == "PAID":
        return {"error": -4, "error_note": "Already paid"}
    if order.status == "CANCELLED":
        return {"error": -9, "error_note": "Transaction cancelled"}
    if abs(float(amount) - float(order.amount_uzs)) > 0.01:
        return {"error": -2, "error_note": "Incorrect parameter amount"}

    if click_error < 0:
        order.status = "CANCELLED"
        order.provider_state = -1
        db.add(order)
        db.commit()
        return {"error": -9, "error_note": "Transaction cancelled"}

    order.status = "PAID"
    order.provider_state = 2
    order.paid_at = datetime.now(timezone.utc)
    unlock_course_for_order(db, order)
    db.add(order)
    db.commit()

    return {
        "click_trans_id": int(click_trans_id) if click_trans_id.isdigit() else click_trans_id,
        "merchant_trans_id": order.order_uid,
        "merchant_confirm_id": order.id,
        "error": 0,
        "error_note": "Success",
    }

import base64
import time
from datetime import datetime, timezone
from typing import Any, Dict, Optional

from sqlalchemy.orm import Session

from app.core.config import get_settings
from app.db.models import PaymentOrder, UserCoursePurchase

# Standard Payme JSON-RPC error codes
ERR_INVALID_AMOUNT = -31001
ERR_TX_NOT_FOUND = -31003
ERR_CANNOT_CANCEL = -31007
ERR_CANNOT_PERFORM = -31008
ERR_ORDER_NOT_FOUND = -31050
ERR_ORDER_ALREADY_PAID = -31051
ERR_UNAUTHORIZED = -32504
ERR_METHOD_NOT_FOUND = -32601


def _now_ms() -> int:
    return int(time.time() * 1000)


def _rpc_error(req_id: Any, code: int, msg_ru: str, msg_uz: str, msg_en: str) -> dict:
    return {
        "jsonrpc": "2.0",
        "id": req_id,
        "error": {
            "code": code,
            "message": {"ru": msg_ru, "uz": msg_uz, "en": msg_en},
        },
    }


def _rpc_result(req_id: Any, result: Dict[str, Any]) -> dict:
    return {"jsonrpc": "2.0", "id": req_id, "result": result}


def verify_payme_basic_auth(auth_header: Optional[str]) -> bool:
    if not auth_header or not auth_header.startswith("Basic "):
        return False
    try:
        decoded = base64.b64decode(auth_header.split(" ", 1)[1].strip()).decode("utf-8")
        login, key = decoded.split(":", 1)
    except Exception:
        return False
    return login == "Paycom" and key == get_settings().payme_merchant_key


def build_payme_checkout_url(order: PaymentOrder) -> str:
    settings = get_settings()
    raw = f"m={settings.payme_merchant_id};ac.order_id={order.order_uid};a={order.amount_tiyin}"
    encoded = base64.b64encode(raw.encode("utf-8")).decode("ascii")
    return f"{settings.payme_checkout_url.rstrip('/')}/{encoded}"


def unlock_course_for_order(db: Session, order: PaymentOrder) -> None:
    existing = (
        db.query(UserCoursePurchase)
        .filter(
            UserCoursePurchase.user_id == order.user_id,
            UserCoursePurchase.course_id == order.course_id,
        )
        .first()
    )
    if not existing:
        db.add(
            UserCoursePurchase(
                user_id=order.user_id,
                course_id=order.course_id,
                provider=order.provider,
            )
        )


def handle_payme_rpc(db: Session, body: Dict[str, Any], auth_header: Optional[str]) -> dict:
    req_id = body.get("id")
    if not verify_payme_basic_auth(auth_header):
        return _rpc_error(req_id, ERR_UNAUTHORIZED, "Ошибка авторизации", "Avtorizatsiya xatosi", "Unauthorized")

    method = body.get("method")
    params = body.get("params") or {}

    if method == "CheckPerformTransaction":
        return _check_perform(db, req_id, params)
    if method == "CreateTransaction":
        return _create_tx(db, req_id, params)
    if method == "PerformTransaction":
        return _perform_tx(db, req_id, params)
    if method == "CancelTransaction":
        return _cancel_tx(db, req_id, params)
    if method == "CheckTransaction":
        return _check_tx(db, req_id, params)
    if method == "GetStatement":
        return _get_statement(db, req_id, params)

    return _rpc_error(req_id, ERR_METHOD_NOT_FOUND, "Метод не найден", "Metod topilmadi", "Method not found")


def _find_order(db: Session, params: Dict[str, Any]) -> Optional[PaymentOrder]:
    account = params.get("account") or {}
    order_uid = account.get("order_id")
    if not order_uid:
        return None
    return db.query(PaymentOrder).filter(PaymentOrder.order_uid == str(order_uid)).first()


def _check_perform(db: Session, req_id: Any, params: Dict[str, Any]) -> dict:
    order = _find_order(db, params)
    if not order:
        return _rpc_error(req_id, ERR_ORDER_NOT_FOUND, "Заказ не найден", "Buyurtma topilmadi", "Order not found")
    if order.status == "PAID":
        return _rpc_error(req_id, ERR_ORDER_ALREADY_PAID, "Заказ уже оплачен", "Buyurtma allaqachon to'langan", "Order already paid")
    if int(params.get("amount", 0)) != order.amount_tiyin:
        return _rpc_error(req_id, ERR_INVALID_AMOUNT, "Неверная сумма", "Noto'g'ri summa", "Invalid amount")
    return _rpc_result(req_id, {"allow": True})


def _create_tx(db: Session, req_id: Any, params: Dict[str, Any]) -> dict:
    tx_id = str(params.get("id", ""))
    existing_tx = db.query(PaymentOrder).filter(PaymentOrder.provider_tx_id == tx_id).first()
    if existing_tx:
        if existing_tx.provider_state != 1:
            return _rpc_error(req_id, ERR_CANNOT_PERFORM, "Невозможно выполнить операцию", "Amalni bajarib bo'lmaydi", "Cannot perform")
        return _rpc_result(
            req_id,
            {
                "create_time": existing_tx.provider_create_time,
                "transaction": existing_tx.order_uid,
                "state": existing_tx.provider_state,
            },
        )

    order = _find_order(db, params)
    if not order:
        return _rpc_error(req_id, ERR_ORDER_NOT_FOUND, "Заказ не найден", "Buyurtma topilmadi", "Order not found")
    if order.status == "PAID":
        return _rpc_error(req_id, ERR_ORDER_ALREADY_PAID, "Заказ уже оплачен", "Buyurtma to'langan", "Order already paid")
    if int(params.get("amount", 0)) != order.amount_tiyin:
        return _rpc_error(req_id, ERR_INVALID_AMOUNT, "Неверная сумма", "Noto'g'ri summa", "Invalid amount")
    if order.provider_tx_id and order.provider_tx_id != tx_id:
        return _rpc_error(req_id, ERR_CANNOT_PERFORM, "Заказ занят другой транзакцией", "Buyurtma band", "Order busy")

    create_time = int(params.get("time") or _now_ms())
    order.provider_tx_id = tx_id
    order.provider_state = 1
    order.provider_create_time = create_time
    db.add(order)
    db.commit()
    return _rpc_result(
        req_id,
        {
            "create_time": order.provider_create_time,
            "transaction": order.order_uid,
            "state": order.provider_state,
        },
    )


def _perform_tx(db: Session, req_id: Any, params: Dict[str, Any]) -> dict:
    tx_id = str(params.get("id", ""))
    order = db.query(PaymentOrder).filter(PaymentOrder.provider_tx_id == tx_id).first()
    if not order:
        return _rpc_error(req_id, ERR_TX_NOT_FOUND, "Транзакция не найдена", "Tranzaksiya topilmadi", "Transaction not found")

    if order.provider_state == 2:
        return _rpc_result(
            req_id,
            {
                "transaction": order.order_uid,
                "perform_time": order.provider_perform_time,
                "state": 2,
            },
        )

    if order.provider_state != 1:
        return _rpc_error(req_id, ERR_CANNOT_PERFORM, "Невозможно провести транзакцию", "Amalni bajarib bo'lmaydi", "Cannot perform")

    order.provider_state = 2
    order.provider_perform_time = _now_ms()
    order.status = "PAID"
    order.paid_at = datetime.now(timezone.utc)
    unlock_course_for_order(db, order)
    db.add(order)
    db.commit()
    return _rpc_result(
        req_id,
        {
            "transaction": order.order_uid,
            "perform_time": order.provider_perform_time,
            "state": 2,
        },
    )


def _cancel_tx(db: Session, req_id: Any, params: Dict[str, Any]) -> dict:
    tx_id = str(params.get("id", ""))
    reason = int(params.get("reason", 1))
    order = db.query(PaymentOrder).filter(PaymentOrder.provider_tx_id == tx_id).first()
    if not order:
        return _rpc_error(req_id, ERR_TX_NOT_FOUND, "Транзакция не найдена", "Tranzaksiya topilmadi", "Transaction not found")

    if order.provider_state in (-1, -2):
        return _rpc_result(
            req_id,
            {
                "transaction": order.order_uid,
                "cancel_time": order.provider_cancel_time,
                "state": order.provider_state,
            },
        )

    order.provider_cancel_time = _now_ms()
    order.cancel_reason = reason
    if order.provider_state == 1:
        order.provider_state = -1
        order.status = "CANCELLED"
    elif order.provider_state == 2:
        order.provider_state = -2
        order.status = "REFUNDED"
    else:
        return _rpc_error(req_id, ERR_CANNOT_CANCEL, "Невозможно отменить", "Bekor qilib bo'lmaydi", "Cannot cancel")

    db.add(order)
    db.commit()
    return _rpc_result(
        req_id,
        {
            "transaction": order.order_uid,
            "cancel_time": order.provider_cancel_time,
            "state": order.provider_state,
        },
    )


def _check_tx(db: Session, req_id: Any, params: Dict[str, Any]) -> dict:
    tx_id = str(params.get("id", ""))
    order = db.query(PaymentOrder).filter(PaymentOrder.provider_tx_id == tx_id).first()
    if not order:
        return _rpc_error(req_id, ERR_TX_NOT_FOUND, "Транзакция не найдена", "Tranzaksiya topilmadi", "Transaction not found")
    return _rpc_result(
        req_id,
        {
            "create_time": order.provider_create_time,
            "perform_time": order.provider_perform_time,
            "cancel_time": order.provider_cancel_time,
            "transaction": order.order_uid,
            "state": order.provider_state,
            "reason": order.cancel_reason,
        },
    )


def _get_statement(db: Session, req_id: Any, params: Dict[str, Any]) -> dict:
    t_from = int(params.get("from", 0))
    t_to = int(params.get("to", _now_ms()))
    orders = (
        db.query(PaymentOrder)
        .filter(
            PaymentOrder.provider == "PAYME",
            PaymentOrder.provider_create_time >= t_from,
            PaymentOrder.provider_create_time <= t_to,
        )
        .order_by(PaymentOrder.provider_create_time.asc())
        .all()
    )
    return _rpc_result(
        req_id,
        {
            "transactions": [
                {
                    "id": o.provider_tx_id,
                    "time": o.provider_create_time,
                    "amount": o.amount_tiyin,
                    "account": {"order_id": o.order_uid},
                    "create_time": o.provider_create_time,
                    "perform_time": o.provider_perform_time,
                    "cancel_time": o.provider_cancel_time,
                    "transaction": o.order_uid,
                    "state": o.provider_state,
                    "reason": o.cancel_reason,
                }
                for o in orders
            ]
        },
    )

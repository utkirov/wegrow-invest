from datetime import datetime, timezone
from typing import Optional

from fastapi import APIRouter, Depends, HTTPException, Query
from pydantic import BaseModel, Field
from sqlalchemy.orm import Session

from app.core.security import get_optional_user
from app.db.models import (
    PortfolioTransaction,
    PriceAlert,
    PurificationRecord,
    Stock,
    User,
    WatchlistItem,
)
from app.db.seed import ensure_default_user
from app.db.session import get_db
from app.services.market_data import stock_to_dict
from app.services.portfolio_service import (
    build_portfolio_summary,
    build_purification_ledger,
    evaluate_user_alerts,
    get_position_quantity,
)

router = APIRouter(prefix="/api/v1", tags=["portfolio"])


class TransactionCreate(BaseModel):
    symbol: str = Field(..., min_length=1, max_length=16)
    side: str = Field(default="BUY", pattern="^(BUY|SELL|buy|sell)$")
    quantity: float = Field(..., gt=0)
    price: float = Field(..., gt=0)
    fee: float = Field(default=0.0, ge=0)
    note: str = Field(default="", max_length=255)


class AlertCreate(BaseModel):
    symbol: str = Field(..., min_length=1, max_length=16)
    condition: str = Field(default="ABOVE", pattern="^(ABOVE|BELOW|HALAL_CHANGE)$")
    target_price: float = Field(default=0.0, ge=0)


class PurificationClearBody(BaseModel):
    symbol: str = Field(..., min_length=1, max_length=16)
    amount: float = Field(..., gt=0)
    quarter_label: str = Field(default="Q4 2026")
    shares: float = Field(default=0.0, ge=0)
    dividend_total: float = Field(default=0.0, ge=0)


def _resolve_user(db: Session, optional_user: Optional[User]) -> User:
    if optional_user is not None:
        return optional_user
    user = ensure_default_user(db)
    db.commit()
    return user


@router.get("/portfolio")
def get_portfolio(
    optional_user: Optional[User] = Depends(get_optional_user),
    db: Session = Depends(get_db),
):
    user = _resolve_user(db, optional_user)
    return build_portfolio_summary(db, user.id)


@router.post("/portfolio/transactions")
def add_transaction(
    body: TransactionCreate,
    optional_user: Optional[User] = Depends(get_optional_user),
    db: Session = Depends(get_db),
):
    user = _resolve_user(db, optional_user)
    symbol = body.symbol.strip().upper()
    stock = db.query(Stock).filter(Stock.symbol == symbol).first()
    if not stock:
        raise HTTPException(status_code=404, detail=f"Stock {symbol} not found")

    side = body.side.upper()
    if side == "SELL":
        held = get_position_quantity(db, user.id, symbol)
        if body.quantity > held + 1e-6:
            raise HTTPException(
                status_code=400,
                detail=f"Insufficient shares of {symbol}: held {held}, attempted to sell {body.quantity}",
            )

    tx = PortfolioTransaction(
        user_id=user.id,
        symbol=symbol,
        side=side,
        quantity=body.quantity,
        price=body.price,
        fee=body.fee,
        note=body.note,
        executed_at=datetime.now(timezone.utc),
    )
    db.add(tx)
    db.commit()
    db.refresh(tx)
    return {
        "status": "ok",
        "transaction_id": tx.id,
        "portfolio": build_portfolio_summary(db, user.id),
    }


@router.delete("/portfolio/transactions/{tx_id}")
def delete_transaction(
    tx_id: int,
    optional_user: Optional[User] = Depends(get_optional_user),
    db: Session = Depends(get_db),
):
    user = _resolve_user(db, optional_user)
    deleted = (
        db.query(PortfolioTransaction)
        .filter(PortfolioTransaction.id == tx_id, PortfolioTransaction.user_id == user.id)
        .delete()
    )
    if not deleted:
        raise HTTPException(status_code=404, detail="Transaction not found")
    db.commit()
    return {"status": "ok", "portfolio": build_portfolio_summary(db, user.id)}


@router.get("/watchlist")
def get_watchlist(
    optional_user: Optional[User] = Depends(get_optional_user),
    db: Session = Depends(get_db),
):
    user = _resolve_user(db, optional_user)
    rows = (
        db.query(WatchlistItem)
        .filter(WatchlistItem.user_id == user.id)
        .order_by(WatchlistItem.created_at.desc())
        .all()
    )
    symbols = [r.symbol for r in rows]
    stock_map = {s.symbol: s for s in db.query(Stock).filter(Stock.symbol.in_(symbols)).all()} if symbols else {}
    return {
        "symbols": symbols,
        "stocks": [stock_to_dict(stock_map[sym]) for sym in symbols if sym in stock_map],
    }


@router.post("/watchlist/{symbol}")
def add_watchlist(
    symbol: str,
    optional_user: Optional[User] = Depends(get_optional_user),
    db: Session = Depends(get_db),
):
    user = _resolve_user(db, optional_user)
    sym = symbol.strip().upper()
    if not db.query(Stock).filter(Stock.symbol == sym).first():
        raise HTTPException(status_code=404, detail="Stock not found")
    exists = (
        db.query(WatchlistItem)
        .filter(WatchlistItem.user_id == user.id, WatchlistItem.symbol == sym)
        .first()
    )
    if not exists:
        db.add(WatchlistItem(user_id=user.id, symbol=sym))
        db.commit()
    return {"status": "ok", "symbol": sym}


@router.delete("/watchlist/{symbol}")
def remove_watchlist(
    symbol: str,
    optional_user: Optional[User] = Depends(get_optional_user),
    db: Session = Depends(get_db),
):
    user = _resolve_user(db, optional_user)
    sym = symbol.strip().upper()
    db.query(WatchlistItem).filter(
        WatchlistItem.user_id == user.id,
        WatchlistItem.symbol == sym,
    ).delete()
    db.commit()
    return {"status": "ok", "symbol": sym}


@router.get("/alerts")
def list_alerts(
    optional_user: Optional[User] = Depends(get_optional_user),
    db: Session = Depends(get_db),
):
    user = _resolve_user(db, optional_user)
    return {"alerts": evaluate_user_alerts(db, user.id)}


@router.post("/alerts")
def create_alert(
    body: AlertCreate,
    optional_user: Optional[User] = Depends(get_optional_user),
    db: Session = Depends(get_db),
):
    user = _resolve_user(db, optional_user)
    sym = body.symbol.strip().upper()
    if not db.query(Stock).filter(Stock.symbol == sym).first():
        raise HTTPException(status_code=404, detail="Stock not found")
    alert = PriceAlert(
        user_id=user.id,
        symbol=sym,
        condition=body.condition,
        target_price=body.target_price,
        active=True,
        triggered=False,
    )
    db.add(alert)
    db.commit()
    db.refresh(alert)
    alerts = evaluate_user_alerts(db, user.id)
    created = next((a for a in alerts if a["id"] == alert.id), None)
    return {"status": "ok", "alert": created}


@router.delete("/alerts/{alert_id}")
def delete_alert(
    alert_id: int,
    optional_user: Optional[User] = Depends(get_optional_user),
    db: Session = Depends(get_db),
):
    user = _resolve_user(db, optional_user)
    deleted = (
        db.query(PriceAlert)
        .filter(PriceAlert.id == alert_id, PriceAlert.user_id == user.id)
        .delete()
    )
    if not deleted:
        raise HTTPException(status_code=404, detail="Alert not found")
    db.commit()
    return {"status": "ok"}


@router.get("/purification")
def get_purification(
    quarter: str = Query(default="Q4 2026"),
    optional_user: Optional[User] = Depends(get_optional_user),
    db: Session = Depends(get_db),
):
    user = _resolve_user(db, optional_user)
    return build_purification_ledger(db, user.id, quarter_label=quarter)


@router.post("/purification/clear")
def clear_purification(
    body: PurificationClearBody,
    optional_user: Optional[User] = Depends(get_optional_user),
    db: Session = Depends(get_db),
):
    user = _resolve_user(db, optional_user)
    sym = body.symbol.strip().upper()
    stock = db.query(Stock).filter(Stock.symbol == sym).first()
    rec = PurificationRecord(
        user_id=user.id,
        symbol=sym,
        quarter_label=body.quarter_label,
        shares=body.shares,
        dividend_total=body.dividend_total,
        impure_pct=stock.impure_revenue_pct if stock else 0.0,
        purification_amount=round(body.amount, 4),
        status="CLEARED",
        cleared_at=datetime.now(timezone.utc),
    )
    db.add(rec)
    db.commit()
    return {
        "status": "ok",
        "ledger": build_purification_ledger(db, user.id, quarter_label=body.quarter_label),
    }

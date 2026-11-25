from dataclasses import dataclass
from datetime import datetime, timezone
from typing import Dict, List

from sqlalchemy.orm import Session

from app.db.models import (
    PortfolioTransaction,
    PriceAlert,
    PurificationRecord,
    Stock,
)


@dataclass
class _LotState:
    quantity: float = 0.0
    total_cost: float = 0.0
    realized_pnl: float = 0.0


def compute_holdings_from_transactions(txs: List[PortfolioTransaction]) -> Dict[str, _LotState]:
    """Compute weighted-average cost holdings and realized P&L from ordered transactions."""
    state: Dict[str, _LotState] = {}
    for tx in sorted(txs, key=lambda t: (t.executed_at or datetime.min.replace(tzinfo=timezone.utc), t.id)):
        sym = tx.symbol.upper()
        lot = state.setdefault(sym, _LotState())
        if tx.side.upper() == "BUY":
            lot.quantity += tx.quantity
            lot.total_cost += (tx.quantity * tx.price) + tx.fee
        else:
            avg_cost = (lot.total_cost / lot.quantity) if lot.quantity > 0 else 0.0
            sell_qty = min(tx.quantity, lot.quantity)
            proceeds = (sell_qty * tx.price) - tx.fee
            lot.realized_pnl += proceeds - (sell_qty * avg_cost)
            lot.quantity = max(0.0, lot.quantity - sell_qty)
            lot.total_cost = max(0.0, lot.quantity * avg_cost)
    return state


def get_position_quantity(db: Session, user_id: int, symbol: str) -> float:
    txs = (
        db.query(PortfolioTransaction)
        .filter(PortfolioTransaction.user_id == user_id, PortfolioTransaction.symbol == symbol.upper())
        .all()
    )
    lots = compute_holdings_from_transactions(txs)
    return round(lots.get(symbol.upper(), _LotState()).quantity, 6)


def build_portfolio_summary(db: Session, user_id: int) -> dict:
    txs = (
        db.query(PortfolioTransaction)
        .filter(PortfolioTransaction.user_id == user_id)
        .order_by(PortfolioTransaction.executed_at.asc(), PortfolioTransaction.id.asc())
        .all()
    )
    lots = compute_holdings_from_transactions(txs)
    symbols = [sym for sym, lot in lots.items() if lot.quantity > 1e-6]
    stock_map: Dict[str, Stock] = {}
    if symbols:
        for st in db.query(Stock).filter(Stock.symbol.in_(symbols)).all():
            stock_map[st.symbol] = st

    positions = []
    total_value = 0.0
    total_invested = 0.0
    total_daily_pnl = 0.0
    total_realized_pnl = sum(lot.realized_pnl for lot in lots.values())
    halal_value = 0.0
    sector_totals: Dict[str, float] = {}
    non_halal_warnings = []

    for sym in sorted(symbols):
        lot = lots[sym]
        st = stock_map.get(sym)
        if not st:
            continue
        avg_price = lot.total_cost / lot.quantity if lot.quantity > 0 else 0.0
        market_value = lot.quantity * st.price
        unrealized_pnl = market_value - lot.total_cost
        unrealized_pct = (unrealized_pnl / lot.total_cost * 100.0) if lot.total_cost > 0 else 0.0
        daily_pnl = lot.quantity * st.change_abs

        total_value += market_value
        total_invested += lot.total_cost
        total_daily_pnl += daily_pnl
        if st.halal_status == "HALAL":
            halal_value += market_value
        else:
            non_halal_warnings.append(
                {
                    "symbol": st.symbol,
                    "name": st.name,
                    "halal_status": st.halal_status,
                    "market_value": round(market_value, 2),
                    "reason": st.verdict_summary,
                }
            )

        sector_totals[st.sector] = sector_totals.get(st.sector, 0.0) + market_value

        positions.append(
            {
                "symbol": st.symbol,
                "name": st.name,
                "sector": st.sector,
                "sector_uz": st.sector_uz,
                "halal_status": st.halal_status,
                "quantity": round(lot.quantity, 4),
                "avg_price": round(avg_price, 2),
                "current_price": round(st.price, 2),
                "invested": round(lot.total_cost, 2),
                "market_value": round(market_value, 2),
                "pnl_abs": round(unrealized_pnl, 2),
                "pnl_pct": round(unrealized_pct, 2),
                "daily_pnl": round(daily_pnl, 2),
                "dividend_per_quarter": st.dividend_per_quarter,
                "purification_per_dividend": st.purification_per_dividend,
            }
        )

    positions.sort(key=lambda p: p["market_value"], reverse=True)
    for pos in positions:
        pos["weight_pct"] = round((pos["market_value"] / total_value * 100.0), 1) if total_value > 0 else 0.0

    total_pnl_abs = total_value - total_invested
    total_pnl_pct = (total_pnl_abs / total_invested * 100.0) if total_invested > 0 else 0.0
    halal_pct = round((halal_value / total_value * 100.0), 1) if total_value > 0 else 100.0

    sector_allocation = [
        {
            "sector": sec,
            "value": round(val, 2),
            "weight_pct": round((val / total_value * 100.0), 1) if total_value > 0 else 0.0,
        }
        for sec, val in sorted(sector_totals.items(), key=lambda kv: kv[1], reverse=True)
    ]

    recent_transactions = [
        {
            "id": t.id,
            "symbol": t.symbol,
            "side": t.side,
            "quantity": t.quantity,
            "price": t.price,
            "fee": t.fee,
            "note": t.note,
            "executed_at": t.executed_at.isoformat() if t.executed_at else None,
        }
        for t in reversed(txs[-20:])
    ]

    return {
        "summary": {
            "total_value": round(total_value, 2),
            "total_invested": round(total_invested, 2),
            "total_pnl_abs": round(total_pnl_abs, 2),
            "total_pnl_pct": round(total_pnl_pct, 2),
            "daily_pnl_abs": round(total_daily_pnl, 2),
            "realized_pnl_abs": round(total_realized_pnl, 2),
            "halal_compliance_pct": halal_pct,
            "positions_count": len(positions),
        },
        "positions": positions,
        "sector_allocation": sector_allocation,
        "non_halal_warnings": non_halal_warnings,
        "transactions": recent_transactions,
    }


def build_purification_ledger(db: Session, user_id: int, quarter_label: str = "Q4 2026") -> dict:
    portfolio = build_portfolio_summary(db, user_id)
    cleared_rows = (
        db.query(PurificationRecord)
        .filter(PurificationRecord.user_id == user_id)
        .order_by(PurificationRecord.created_at.desc())
        .all()
    )
    cleared_by_symbol_quarter: Dict[tuple[str, str], float] = {}
    for rec in cleared_rows:
        if rec.status == "CLEARED":
            key = (rec.symbol.upper(), rec.quarter_label)
            cleared_by_symbol_quarter[key] = cleared_by_symbol_quarter.get(key, 0.0) + rec.purification_amount

    items = []
    total_due = 0.0
    total_cleared_quarter = 0.0

    for pos in portfolio["positions"]:
        if pos["halal_status"] == "HARAM":
            continue
        div_total = round(pos["quantity"] * pos["dividend_per_quarter"], 4)
        due_amount = round(pos["quantity"] * pos["purification_per_dividend"], 4)
        cleared_amt = round(cleared_by_symbol_quarter.get((pos["symbol"], quarter_label), 0.0), 4)
        remaining = round(max(0.0, due_amount - cleared_amt), 4)
        total_due += remaining
        total_cleared_quarter += cleared_amt

        if due_amount > 0 or cleared_amt > 0:
            items.append(
                {
                    "symbol": pos["symbol"],
                    "name": pos["name"],
                    "quarter_label": quarter_label,
                    "shares": pos["quantity"],
                    "dividend_total": div_total,
                    "purification_due": due_amount,
                    "cleared_amount": cleared_amt,
                    "remaining_amount": remaining,
                    "status": "CLEARED" if remaining <= 0.0001 else "PENDING",
                }
            )

    history = [
        {
            "id": r.id,
            "symbol": r.symbol,
            "quarter_label": r.quarter_label,
            "shares": r.shares,
            "dividend_total": r.dividend_total,
            "purification_amount": r.purification_amount,
            "status": r.status,
            "cleared_at": r.cleared_at.isoformat() if r.cleared_at else None,
        }
        for r in cleared_rows[:30]
    ]

    return {
        "quarter_label": quarter_label,
        "total_pending_usd": round(total_due, 4),
        "total_cleared_usd": round(total_cleared_quarter, 4),
        "items": items,
        "history": history,
    }


def evaluate_user_alerts(db: Session, user_id: int) -> List[dict]:
    alerts = (
        db.query(PriceAlert)
        .filter(PriceAlert.user_id == user_id)
        .order_by(PriceAlert.id.desc())
        .all()
    )
    symbols = list({a.symbol for a in alerts})
    stock_map = {
        s.symbol: s for s in db.query(Stock).filter(Stock.symbol.in_(symbols)).all()
    } if symbols else {}

    now = datetime.now(timezone.utc)
    dirty = False
    for alert in alerts:
        if not alert.active or alert.triggered:
            continue
        st = stock_map.get(alert.symbol)
        if not st:
            continue
        fired = False
        msg = ""
        if alert.condition == "ABOVE" and st.price >= alert.target_price:
            fired = True
            msg = f"{st.symbol} достиг ${st.price:.2f} (цель ≥ ${alert.target_price:.2f})"
        elif alert.condition == "BELOW" and st.price <= alert.target_price:
            fired = True
            msg = f"{st.symbol} снизился до ${st.price:.2f} (цель ≤ ${alert.target_price:.2f})"
        elif alert.condition == "HALAL_CHANGE" and st.halal_status != "HALAL":
            fired = True
            msg = f"Внимание: статус {st.symbol} изменился на {st.halal_status}"

        if fired:
            alert.triggered = True
            alert.active = False
            alert.triggered_at = now
            alert.message = msg
            db.add(alert)
            dirty = True

    if dirty:
        db.commit()

    return [
        {
            "id": a.id,
            "symbol": a.symbol,
            "condition": a.condition,
            "target_price": a.target_price,
            "active": a.active,
            "triggered": a.triggered,
            "triggered_at": a.triggered_at.isoformat() if a.triggered_at else None,
            "message": a.message,
            "current_price": round(stock_map[a.symbol].price, 2) if a.symbol in stock_map else None,
        }
        for a in alerts
    ]

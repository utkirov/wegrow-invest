from typing import Optional

from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy import asc, desc, or_
from sqlalchemy.orm import Session

from app.db.models import Stock
from app.db.session import get_db
from app.services.market_data import (
    VALID_TIMEFRAMES,
    MarketDataService,
    build_market_overview,
    stock_to_dict,
)

router = APIRouter(prefix="/api/v1", tags=["market"])
_market_service = MarketDataService()


@router.get("/market/overview")
def market_overview(db: Session = Depends(get_db)):
    return build_market_overview(db)


@router.get("/stocks")
def list_stocks_v1(
    q: Optional[str] = Query(default=None, description="Search by ticker, name, sector, or industry"),
    halal_status: Optional[str] = Query(default=None, description="HALAL | DOUBTFUL | HARAM"),
    halal_only: bool = Query(default=False),
    sector: Optional[str] = Query(default=None),
    dividend_only: bool = Query(default=False),
    sort_by: str = Query(default="market_cap", description="market_cap | change_pct | price | symbol"),
    page: int = Query(default=1, ge=1),
    page_size: int = Query(default=30, ge=1, le=200),
    db: Session = Depends(get_db),
):
    query = db.query(Stock)

    if halal_only:
        query = query.filter(Stock.halal_status == "HALAL")
    elif halal_status and halal_status.upper() in {"HALAL", "DOUBTFUL", "HARAM"}:
        query = query.filter(Stock.halal_status == halal_status.upper())

    if sector and sector not in ("Все", "Barchasi", "ALL"):
        query = query.filter(
            or_(
                Stock.sector.ilike(sector),
                Stock.sector_uz.ilike(sector),
            )
        )

    if dividend_only:
        query = query.filter(Stock.dividend_per_quarter > 0.0)

    if q and q.strip():
        pattern = f"%{q.strip()}%"
        query = query.filter(
            or_(
                Stock.symbol.ilike(pattern),
                Stock.name.ilike(pattern),
                Stock.sector.ilike(pattern),
                Stock.sector_uz.ilike(pattern),
                Stock.industry.ilike(pattern),
            )
        )

    total = query.count()

    sort_map = {
        "market_cap": desc(Stock.market_cap_usd),
        "change_pct": desc(Stock.change_pct),
        "price": desc(Stock.price),
        "symbol": asc(Stock.symbol),
    }
    query = query.order_by(sort_map.get(sort_by, desc(Stock.market_cap_usd)))

    items = query.offset((page - 1) * page_size).limit(page_size).all()

    sectors = sorted({row[0] for row in db.query(Stock.sector).distinct().all()})
    return {
        "stocks": [stock_to_dict(st) for st in items],
        "pagination": {
            "page": page,
            "page_size": page_size,
            "total": total,
            "total_pages": max(1, (total + page_size - 1) // page_size),
        },
        "sectors": sectors,
    }


@router.get("/stocks/{symbol}")
def get_stock_v1(
    symbol: str,
    timeframe: str = Query(default="1M"),
    refresh: bool = Query(default=False),
    db: Session = Depends(get_db),
):
    stock = db.query(Stock).filter(Stock.symbol == symbol.strip().upper()).first()
    if not stock:
        raise HTTPException(status_code=404, detail="Stock not found")
    if refresh:
        stock = _market_service.refresh_stock(db, stock, force=True)
    return stock_to_dict(stock, timeframe=timeframe)


@router.get("/stocks/{symbol}/chart")
def get_stock_chart_v1(
    symbol: str,
    timeframe: str = Query(default="1M"),
    refresh: bool = Query(default=False),
    db: Session = Depends(get_db),
):
    tf = timeframe.upper()
    if tf not in VALID_TIMEFRAMES:
        raise HTTPException(
            status_code=422,
            detail=f"Invalid timeframe '{timeframe}'. Allowed: {', '.join(VALID_TIMEFRAMES)}",
        )
    stock = db.query(Stock).filter(Stock.symbol == symbol.strip().upper()).first()
    if not stock:
        raise HTTPException(status_code=404, detail="Stock not found")
    return _market_service.get_chart(db, stock, timeframe=tf, refresh=refresh)


@router.post("/stocks/{symbol}/refresh")
def refresh_stock_v1(symbol: str, db: Session = Depends(get_db)):
    stock = db.query(Stock).filter(Stock.symbol == symbol.strip().upper()).first()
    if not stock:
        raise HTTPException(status_code=404, detail="Stock not found")
    stock = _market_service.refresh_stock(db, stock, force=True)
    return stock_to_dict(stock)

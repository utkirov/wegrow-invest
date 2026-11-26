from typing import Optional

from fastapi import APIRouter, HTTPException, Query

from app.services.stock_universe import (
    INITIAL_BROKERS,
    INITIAL_NEWS,
    INITIAL_STORIES,
    INITIAL_STREAMS,
)

router = APIRouter(prefix="/api/v1", tags=["content"])


@router.get("/news")
def list_news(
    tag: Optional[str] = Query(default=None),
    ticker: Optional[str] = Query(default=None),
    q: Optional[str] = Query(default=None),
):
    items = list(INITIAL_NEWS)
    if tag and tag.strip() and tag.strip().lower() not in {"все", "all", "barchasi"}:
        tl = tag.strip().lower()
        items = [n for n in items if n.get("tag", "").lower() == tl]
    if ticker and ticker.strip():
        tk = ticker.strip().upper()
        items = [n for n in items if n.get("ticker", "").upper() == tk]
    if q and q.strip():
        ql = q.strip().lower()
        items = [
            n
            for n in items
            if ql in n.get("title", "").lower()
            or ql in n.get("title_uz", "").lower()
            or ql in n.get("summary", "").lower()
            or ql in n.get("ticker", "").lower()
        ]
    return {"news": items, "total": len(items)}


@router.get("/news/{news_id}")
def get_news_detail(news_id: int):
    for item in INITIAL_NEWS:
        if item["id"] == news_id:
            return {"article": item}
    raise HTTPException(status_code=404, detail="Article not found")


@router.get("/stories")
def list_stories():
    return {"stories": INITIAL_STORIES}


@router.get("/brokers")
def list_brokers(
    us_stocks: Optional[bool] = Query(default=None),
    uz_card: Optional[bool] = Query(default=None),
):
    items = list(INITIAL_BROKERS)
    if us_stocks is not None:
        items = [b for b in items if bool(b.get("us_stocks")) == us_stocks]
    if uz_card is not None:
        items = [b for b in items if bool(b.get("uz_card_funding")) == uz_card]
    return {"brokers": items, "total": len(items)}


@router.get("/streams")
def list_streams(status: Optional[str] = Query(default=None)):
    items = list(INITIAL_STREAMS)
    if status and status.strip().upper() in {"LIVE", "UPCOMING", "RECORDED"}:
        st = status.strip().upper()
        items = [s for s in items if s.get("status") == st]
    return {"streams": items, "total": len(items)}

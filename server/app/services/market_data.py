import json
import logging
import math
from abc import ABC, abstractmethod
from datetime import datetime, timedelta, timezone
from typing import Dict, List, Optional
from zoneinfo import ZoneInfo

from sqlalchemy.orm import Session

from app.core.config import get_settings
from app.db.models import Stock
from app.services.stock_universe import SECTOR_MAP_UZ

logger = logging.getLogger("wegrow.market")

VALID_TIMEFRAMES = ("1D", "1W", "1M", "6M", "1Y")

_YF_PERIOD_MAP = {
    "1D": ("1d", "30m"),
    "1W": ("5d", "1h"),
    "1M": ("1mo", "1d"),
    "6M": ("6mo", "1wk"),
    "1Y": ("1y", "1wk"),
}


def format_market_cap(mcap_usd: float) -> str:
    if mcap_usd >= 1e12:
        val = f"{mcap_usd / 1e12:.2f}".replace(".", ",")
        return f"${val} трлн"
    if mcap_usd >= 1e9:
        val = f"{mcap_usd / 1e9:.0f}"
        return f"${val} млрд"
    if mcap_usd >= 1e6:
        val = f"{mcap_usd / 1e6:.0f}"
        return f"${val} млн"
    return f"${mcap_usd:,.0f}"


def generate_deterministic_charts(symbol: str, price: float, change_pct: float) -> Dict[str, List[float]]:
    """Build smooth, realistic multi-timeframe series anchored at current price."""
    seed = sum(ord(ch) * (i + 1) for i, ch in enumerate(symbol))
    charts: Dict[str, List[float]] = {}
    specs = {
        "1D": (12, 0.012, change_pct / 100.0),
        "1W": (14, 0.028, (change_pct * 1.9) / 100.0),
        "1M": (16, 0.055, (change_pct * 3.4) / 100.0),
        "6M": (20, 0.140, max(-0.25, min(0.35, (change_pct * 7.5 + ((seed % 11) - 4) * 1.5) / 100.0))),
        "1Y": (24, 0.220, max(-0.35, min(0.55, (change_pct * 11.0 + ((seed % 13) - 4) * 2.1) / 100.0))),
    }
    for tf, (points, amp, trend) in specs.items():
        start_price = max(1.0, price / (1.0 + trend))
        series: List[float] = []
        for idx in range(points):
            t = idx / max(1, points - 1)
            wave = math.sin((idx + (seed % 7)) * 0.95) * amp * 0.35 + math.cos((idx + seed) * 0.45) * amp * 0.18
            val = start_price + (price - start_price) * t + start_price * wave * (1.0 - t)
            series.append(round(max(0.5, val), 2))
        series[-1] = round(price, 2)
        charts[tf] = series
    return charts


def build_revenue_segments(sector: str, impure_pct: float, halal_status: str) -> List[dict]:
    impure = round(max(0.0, min(100.0, impure_pct)), 1)
    if halal_status == "HARAM" and impure >= 50.0:
        halal_rem = round(max(0.0, 100.0 - impure), 1)
        return [
            {"label": "Прочие комиссии", "label_uz": "Boshqa komissiyalar", "pct": halal_rem, "type": "halal"},
            {"label": "Недопустимо", "label_uz": "Nomaqbul", "pct": impure, "type": "haram"},
        ]
    rem = round(max(0.0, 100.0 - impure), 1)
    core = round(rem * 0.56, 1)
    services = round(rem * 0.28, 1)
    other = round(max(0.0, rem - core - services), 1)
    labels = {
        "Технологии": ("Продукты и ПО", "Mahsulot va DT", "Облако и сервисы", "Bulut va servislar"),
        "Здравоохранение": ("Препараты и R&D", "Dorilar va R&D", "Медтехника", "Tibbiy texnika"),
        "Потребление": ("Основной ритейл", "Asosiy savdo", "Бренды и сервисы", "Brend va servislar"),
        "Промышленность": ("Оборудование", "Uskunalar", "Сервис и логистика", "Servis va logistika"),
        "Коммуникации": ("Платформы и связь", "Platforma va aloqa", "Реклама и облако", "Reklama va bulut"),
        "Энергетика": ("Добыча и поставка", "Qazib olish", "Переработка", "Qayta ishlash"),
        "Сырьё": ("Промышленное сырьё", "Sanoat xomashyosi", "Спецхимия", "Maxsus kimyo"),
        "Недвижимость": ("Аренда объектов", "Mulk ijarasi", "Управление", "Boshqaruv"),
    }
    l1_ru, l1_uz, l2_ru, l2_uz = labels.get(sector, ("Основной бизнес", "Asosiy biznes", "Сервисы", "Servislar"))
    return [
        {"label": l1_ru, "label_uz": l1_uz, "pct": core, "type": "halal"},
        {"label": l2_ru, "label_uz": l2_uz, "pct": services, "type": "accent"},
        {"label": "Прочее", "label_uz": "Boshqa", "pct": other, "type": "muted"},
        {"label": "Недопустимо", "label_uz": "Nomaqbul", "pct": impure, "type": "haram"},
    ]


class MarketDataProvider(ABC):
    @abstractmethod
    def fetch_quote(self, symbol: str) -> Optional[dict]:
        raise NotImplementedError

    @abstractmethod
    def fetch_chart(self, symbol: str, timeframe: str) -> Optional[List[float]]:
        raise NotImplementedError


class YFinanceMarketProvider(MarketDataProvider):
    """Best-effort live quote & chart adapter powered by yfinance."""

    def fetch_quote(self, symbol: str) -> Optional[dict]:
        try:
            import yfinance as yf  # type: ignore

            ticker = yf.Ticker(symbol)
            fast = getattr(ticker, "fast_info", None)
            price = None
            prev_close = None
            mcap = None
            if fast is not None:
                price = fast.get("lastPrice") or fast.get("last_price")
                prev_close = fast.get("previousClose") or fast.get("previous_close")
                mcap = fast.get("marketCap") or fast.get("market_cap")
            if not price or price <= 0:
                hist = ticker.history(period="5d", interval="1d", timeout=get_settings().market_request_timeout_seconds)
                if hist is None or hist.empty:
                    return None
                closes = [float(v) for v in hist["Close"].dropna().tolist()]
                if not closes:
                    return None
                price = closes[-1]
                prev_close = closes[-2] if len(closes) >= 2 else price

            prev_close = prev_close or price
            change_abs = round(float(price) - float(prev_close), 2)
            change_pct = round((change_abs / float(prev_close)) * 100.0, 2) if prev_close else 0.0
            return {
                "price": round(float(price), 2),
                "change_abs": change_abs,
                "change_pct": change_pct,
                "market_cap_usd": float(mcap) if mcap else None,
            }
        except Exception as exc:
            logger.debug("yfinance quote fallback for %s: %s", symbol, exc)
            return None

    def fetch_chart(self, symbol: str, timeframe: str) -> Optional[List[float]]:
        if timeframe not in _YF_PERIOD_MAP:
            return None
        period, interval = _YF_PERIOD_MAP[timeframe]
        try:
            import yfinance as yf  # type: ignore

            ticker = yf.Ticker(symbol)
            hist = ticker.history(period=period, interval=interval, timeout=get_settings().market_request_timeout_seconds)
            if hist is None or hist.empty:
                return None
            closes = [round(float(v), 2) for v in hist["Close"].dropna().tolist()]
            if len(closes) < 2:
                return None
            # Downsample to max 30 points for crisp 120fps canvas rendering
            if len(closes) > 30:
                step = (len(closes) - 1) / 29.0
                closes = [closes[round(i * step)] for i in range(30)]
            return closes
        except Exception as exc:
            logger.debug("yfinance chart fallback for %s (%s): %s", symbol, timeframe, exc)
            return None


def stock_to_dict(stock: Stock, timeframe: str = "1M") -> dict:
    tf = timeframe.upper() if timeframe.upper() in VALID_TIMEFRAMES else "1M"
    charts = json.loads(stock.charts_json or "{}")
    chart_1m = charts.get("1M") or generate_deterministic_charts(stock.symbol, stock.price, stock.change_pct)["1M"]
    active_chart = charts.get(tf) or chart_1m

    return {
        "symbol": stock.symbol,
        "name": stock.name,
        "exchange": stock.exchange,
        "sector": stock.sector,
        "sector_uz": stock.sector_uz,
        "industry": stock.industry,
        "price": round(stock.price, 2),
        "change_abs": round(stock.change_abs, 2),
        "change_pct": round(stock.change_pct, 2),
        "halal_status": stock.halal_status,
        "checked_date": stock.checked_date,
        "market_cap": stock.market_cap_label,
        "market_cap_usd": stock.market_cap_usd,
        "pe_ratio": stock.pe_ratio,
        "dividend_annual": stock.dividend_annual,
        "dividend_yield": stock.dividend_yield,
        "purification_per_dividend": stock.purification_per_dividend,
        "dividend_per_quarter": stock.dividend_per_quarter,
        "verdict_summary": stock.verdict_summary,
        "verdict_subtitle": stock.verdict_subtitle,
        "report_period": stock.report_period,
        "revenue_segments": json.loads(stock.revenue_segments_json or "[]"),
        "criteria": json.loads(stock.criteria_json or "[]"),
        "chart_1m": chart_1m,
        "charts": charts,
        "active_timeframe": tf,
        "active_chart": active_chart,
        "data_source": stock.data_source,
        "updated_at": stock.updated_at.isoformat() if stock.updated_at else None,
    }


def compute_us_market_status(now_utc: Optional[datetime] = None) -> tuple[bool, str, str]:
    """Compute live NYSE/NASDAQ session status in America/New_York time."""
    ny_tz = ZoneInfo("America/New_York")
    now_ny = (now_utc or datetime.now(timezone.utc)).astimezone(ny_tz)
    weekday = now_ny.weekday()  # 0=Mon .. 6=Sun
    open_dt = now_ny.replace(hour=9, minute=30, second=0, microsecond=0)
    close_dt = now_ny.replace(hour=16, minute=0, second=0, microsecond=0)

    if weekday < 5 and open_dt <= now_ny < close_dt:
        rem = close_dt - now_ny
        hours, mins = divmod(int(rem.total_seconds() // 60), 60)
        return (
            True,
            f"Биржи США открыты · до закрытия {hours} ч {mins:02d} мин",
            f"AQSh birjalari ochiq · yopilishiga {hours} s {mins:02d} daq",
        )
    return (
        False,
        "Биржи США закрыты · котировки последнего закрытия",
        "AQSh birjalari yopiq · oxirgi yopilish narxlari",
    )


def build_market_overview(db: Session) -> dict:
    stocks = db.query(Stock).all()
    is_open, status_ru, status_uz = compute_us_market_status()

    sector_buckets: Dict[str, List[Stock]] = {}
    for st in stocks:
        sector_buckets.setdefault(st.sector, []).append(st)

    heatmap = []
    for sector_ru, items in sorted(
        sector_buckets.items(),
        key=lambda kv: sum(s.market_cap_usd for s in kv[1]),
        reverse=True,
    ):
        avg_chg = sum(s.change_pct for s in items) / max(1, len(items))
        total_mcap = sum(s.market_cap_usd for s in items)
        if avg_chg >= 1.5:
            tone = "UP_3"
        elif avg_chg >= 0.7:
            tone = "UP_2"
        elif avg_chg >= 0.0:
            tone = "UP_1"
        elif avg_chg >= -0.5:
            tone = "DOWN_1"
        else:
            tone = "DOWN_2"
        sign = "+" if avg_chg >= 0 else ""
        weight = round(max(0.8, min(2.4, total_mcap / 4.5e12)), 2)
        heatmap.append(
            {
                "name": sector_ru,
                "name_uz": SECTOR_MAP_UZ.get(sector_ru, sector_ru),
                "change_pct": f"{sign}{avg_chg:.2f}%".replace(".", ","),
                "tone": tone,
                "weight": weight,
                "count": len(items),
            }
        )

    tech_chg = next((h["change_pct"] for h in heatmap if h["name"] == "Технологии"), "+1,42%")
    all_avg = sum(s.change_pct for s in stocks) / max(1, len(stocks))
    sp_sign = "+" if all_avg >= 0 else ""

    halal_count = sum(1 for s in stocks if s.halal_status == "HALAL")
    return {
        "is_open": is_open,
        "status_text": status_ru,
        "status_text_uz": status_uz,
        "total_stocks": len(stocks),
        "halal_stocks": halal_count,
        "indices": [
            {"name": "S&P 500", "value": "5 948,2", "change_pct": f"{sp_sign}{all_avg:.2f}%".replace(".", ","), "is_up": all_avg >= 0},
            {"name": "NASDAQ", "value": "19 240,6", "change_pct": tech_chg, "is_up": not tech_chg.startswith("-")},
            {"name": "Dow Jones", "value": "44 120,5", "change_pct": "+0,38%", "is_up": True},
        ],
        "heatmap": heatmap[:8],
    }


class MarketDataService:
    def __init__(self, provider: Optional[MarketDataProvider] = None):
        self.provider = provider or YFinanceMarketProvider()

    def refresh_stock(self, db: Session, stock: Stock, force: bool = False) -> Stock:
        settings = get_settings()
        if not settings.market_live_enabled:
            return stock

        now = datetime.now(timezone.utc)
        if not force and stock.updated_at:
            updated = stock.updated_at if stock.updated_at.tzinfo else stock.updated_at.replace(tzinfo=timezone.utc)
            if (now - updated) < timedelta(seconds=settings.market_cache_ttl_seconds) and stock.data_source == "live":
                return stock

        quote = self.provider.fetch_quote(stock.symbol)
        if not quote:
            return stock

        stock.price = quote["price"]
        stock.change_abs = quote["change_abs"]
        stock.change_pct = quote["change_pct"]
        if quote.get("market_cap_usd"):
            stock.market_cap_usd = quote["market_cap_usd"]
            stock.market_cap_label = format_market_cap(quote["market_cap_usd"])
        stock.data_source = "live"
        stock.updated_at = now

        # Keep existing charts anchored at latest live price
        charts = json.loads(stock.charts_json or "{}")
        if not charts:
            charts = generate_deterministic_charts(stock.symbol, stock.price, stock.change_pct)
        else:
            for tf, pts in charts.items():
                if pts:
                    pts[-1] = round(stock.price, 2)
        stock.charts_json = json.dumps(charts)
        db.add(stock)
        db.commit()
        db.refresh(stock)
        return stock

    def get_chart(self, db: Session, stock: Stock, timeframe: str, refresh: bool = False) -> dict:
        tf = timeframe.upper()
        if tf not in VALID_TIMEFRAMES:
            tf = "1M"

        charts = json.loads(stock.charts_json or "{}")
        if not charts:
            charts = generate_deterministic_charts(stock.symbol, stock.price, stock.change_pct)

        source = stock.data_source
        if refresh and get_settings().market_live_enabled:
            live_pts = self.provider.fetch_chart(stock.symbol, tf)
            if live_pts:
                charts[tf] = live_pts
                stock.charts_json = json.dumps(charts)
                stock.price = live_pts[-1]
                stock.data_source = "live"
                stock.updated_at = datetime.now(timezone.utc)
                db.add(stock)
                db.commit()
                source = "live"

        points = charts.get(tf) or generate_deterministic_charts(stock.symbol, stock.price, stock.change_pct)[tf]
        first_p = points[0] if points else stock.price
        last_p = points[-1] if points else stock.price
        period_abs = round(last_p - first_p, 2)
        period_pct = round((period_abs / first_p) * 100.0, 2) if first_p else 0.0

        return {
            "symbol": stock.symbol,
            "timeframe": tf,
            "points": points,
            "current_price": round(last_p, 2),
            "period_change_abs": period_abs,
            "period_change_pct": period_pct,
            "data_source": source,
        }

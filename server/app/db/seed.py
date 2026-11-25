import json
from datetime import datetime, timezone

from sqlalchemy.orm import Session

from app.db.models import Course, Goal, Stock, User
from app.db.session import Base
from app.services.halal_screener import AaoifiScreeningInput, screen_stock_aaoifi
from app.services.market_data import (
    build_revenue_segments,
    format_market_cap,
    generate_deterministic_charts,
)
from app.services.stock_universe import (
    INITIAL_COURSES,
    INITIAL_GOALS,
    SECTOR_MAP_UZ,
    STOCK_UNIVERSE,
)


def init_db_schema() -> None:
    from app.db import session as db_session

    Base.metadata.create_all(bind=db_session.engine)


def ensure_default_user(db: Session) -> User:
    user = db.get(User, 1)
    if user:
        return user
    user = User(
        id=1,
        phone="+998901234567",
        email="aziz@wegrow.uz",
        first_name="Азиз",
        last_name="Каримов",
        middle_name="Рустамович",
        birth_date="15.04.1996",
        gender="male",
        language="ru",
        dark_theme=True,
        is_guest=False,
    )
    db.add(user)
    db.flush()
    seed_user_default_goals(db, user.id)
    return user


def seed_user_default_goals(db: Session, user_id: int) -> None:
    existing = db.query(Goal).filter(Goal.user_id == user_id).count()
    if existing > 0:
        return
    for g in INITIAL_GOALS:
        db.add(Goal(user_id=user_id, **g))


def seed_stocks(db: Session) -> int:
    existing_symbols = {row[0] for row in db.query(Stock.symbol).all()}
    now = datetime.now(timezone.utc)
    checked_date = now.strftime("%d.%m.%Y")
    added = 0

    for (
        symbol,
        name,
        exchange,
        sector_ru,
        industry,
        price,
        change_pct,
        mcap_bn,
        pe,
        div_annual,
        debt_pct,
        dep_pct,
        impure_pct,
        biz_override,
    ) in STOCK_UNIVERSE:
        if symbol in existing_symbols:
            continue

        div_quarter = round(div_annual / 4.0, 4)
        div_yield = round((div_annual / price) * 100.0, 2) if price > 0 else 0.0
        change_abs = round(price * (change_pct / 100.0) / (1.0 + change_pct / 100.0), 2)
        mcap_usd = mcap_bn * 1e9

        screening = screen_stock_aaoifi(
            AaoifiScreeningInput(
                industry=industry,
                debt_ratio_pct=debt_pct,
                interest_deposit_ratio_pct=dep_pct,
                impure_revenue_pct=impure_pct,
                dividend_per_quarter=div_quarter,
                business_override=biz_override,
            )
        )

        charts = generate_deterministic_charts(symbol, price, change_pct)
        segments = build_revenue_segments(sector_ru, impure_pct, screening.halal_status)

        stock = Stock(
            symbol=symbol,
            name=name,
            exchange=exchange,
            sector=sector_ru,
            sector_uz=SECTOR_MAP_UZ.get(sector_ru, sector_ru),
            industry=industry,
            price=price,
            change_abs=change_abs,
            change_pct=change_pct,
            market_cap_usd=mcap_usd,
            market_cap_label=format_market_cap(mcap_usd),
            pe_ratio=f"{pe:.1f}".replace(".", ","),
            dividend_annual=f"${div_annual:.2f}".replace(".", ","),
            dividend_yield=f"{div_yield:.2f}%".replace(".", ","),
            dividend_per_quarter=div_quarter,
            debt_ratio_pct=debt_pct,
            interest_deposit_ratio_pct=dep_pct,
            impure_revenue_pct=impure_pct,
            business_status=screening.business_status,
            halal_status=screening.halal_status,
            purification_per_dividend=screening.purification_per_dividend,
            verdict_summary=screening.verdict_summary,
            verdict_subtitle=screening.verdict_subtitle,
            report_period="Отчётность за III квартал 2026",
            checked_date=checked_date,
            manual_override=biz_override is not None,
            revenue_segments_json=json.dumps(segments, ensure_ascii=False),
            criteria_json=json.dumps(screening.criteria, ensure_ascii=False),
            charts_json=json.dumps(charts),
            data_source="seed",
            updated_at=now,
        )
        db.add(stock)
        added += 1

    return added


def seed_courses(db: Session) -> None:
    if db.query(Course).count() > 0:
        return
    for course in INITIAL_COURSES:
        db.add(Course(id=course["id"], payload_json=json.dumps(course, ensure_ascii=False)))


def seed_all() -> None:
    from app.db import session as db_session
    from app.services.lms_seed import seed_lessons

    init_db_schema()
    db = db_session.SessionLocal()
    try:
        ensure_default_user(db)
        seed_stocks(db)
        seed_courses(db)
        db.flush()
        seed_lessons(db)
        db.commit()
    finally:
        db.close()

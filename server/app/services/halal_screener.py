from dataclasses import dataclass
from typing import List, Optional

# Standard AAOIFI Sharia Screening Thresholds
AAOIFI_DEBT_LIMIT_PCT = 30.0
AAOIFI_INTEREST_DEPOSIT_LIMIT_PCT = 30.0
AAOIFI_IMPURE_REVENUE_LIMIT_PCT = 5.0

HARAM_INDUSTRIES = {
    "conventional banking",
    "diversified banks",
    "regional banks",
    "investment banking",
    "consumer finance",
    "credit services",
    "insurance",
    "insurance - life",
    "insurance - property & casualty",
    "insurance - diversified",
    "alcohol",
    "beverages - brewers",
    "beverages - wineries & distilleries",
    "gambling",
    "resorts & casinos",
    "tobacco",
    "weapons",
    "aerospace & defense (weapons)",
    "adult entertainment",
    "pork & non-halal meat",
}

DOUBTFUL_INDUSTRIES = {
    "entertainment",
    "streaming & media",
    "broadcasting",
    "mixed retail & media",
    "hotels & hospitality",
    "financial data & exchanges",
}


def _fmt_pct(value: float) -> str:
    return f"{value:.1f}%".replace(".", ",")


@dataclass(frozen=True)
class AaoifiScreeningInput:
    industry: str
    debt_ratio_pct: float
    interest_deposit_ratio_pct: float
    impure_revenue_pct: float
    dividend_per_quarter: float = 0.0
    business_override: Optional[str] = None  # HALAL | DOUBTFUL | HARAM
    business_note_ru: Optional[str] = None
    business_note_uz: Optional[str] = None


@dataclass(frozen=True)
class AaoifiScreeningResult:
    halal_status: str  # HALAL | DOUBTFUL | HARAM
    business_status: str
    purification_per_dividend: float
    verdict_summary: str
    verdict_subtitle: str
    criteria: List[dict]


def evaluate_business_activity(
    industry: str,
    override: Optional[str] = None,
    note_ru: Optional[str] = None,
    note_uz: Optional[str] = None,
) -> tuple[str, str, str, str]:
    """Return (status, value_label, subtitle_ru, subtitle_uz)."""
    normalized = (industry or "").strip().lower()
    if override in {"HALAL", "DOUBTFUL", "HARAM"}:
        status = override
    elif normalized in HARAM_INDUSTRIES or any(
        kw in normalized for kw in ("bank", "casino", "gambl", "tobacco", "alcohol", "brewer", "distill", "insurance")
    ):
        status = "HARAM"
    elif normalized in DOUBTFUL_INDUSTRIES or any(kw in normalized for kw in ("media", "entertainment", "hotel")):
        status = "DOUBTFUL"
    else:
        status = "HALAL"

    if status == "HALAL":
        return "HALAL", "OK", note_ru or "допустима", note_uz or "maqbul"
    if status == "DOUBTFUL":
        return "DOUBTFUL", "Спорно", note_ru or "смешанный сегмент", note_uz or "aralash segment"
    return "HARAM", "Харам", note_ru or "запрещённая отрасль", note_uz or "taqiqlangan soha"


def screen_stock_aaoifi(data: AaoifiScreeningInput) -> AaoifiScreeningResult:
    """Evaluate a US equity against the 4 AAOIFI Sharia screening criteria.

    Strict AAOIFI boundaries:
      - Interest-bearing debt / Market Cap < 30.0%
      - Interest-bearing deposits & securities / Market Cap < 30.0%
      - Non-permissible revenue / Total revenue < 5.0%
    Values strictly below threshold pass (HALAL); values >= threshold fail (HARAM).
    Borderline impure revenue (4.0% <= x < 5.0%) is flagged DOUBTFUL when business is DOUBTFUL.
    """
    biz_status, biz_label, biz_sub_ru, biz_sub_uz = evaluate_business_activity(
        data.industry,
        override=data.business_override,
        note_ru=data.business_note_ru,
        note_uz=data.business_note_uz,
    )

    debt_val = max(0.0, round(float(data.debt_ratio_pct), 2))
    dep_val = max(0.0, round(float(data.interest_deposit_ratio_pct), 2))
    impure_val = max(0.0, round(float(data.impure_revenue_pct), 2))

    debt_status = "HALAL" if debt_val < AAOIFI_DEBT_LIMIT_PCT else "HARAM"
    dep_status = "HALAL" if dep_val < AAOIFI_INTEREST_DEPOSIT_LIMIT_PCT else "HARAM"

    if impure_val >= AAOIFI_IMPURE_REVENUE_LIMIT_PCT:
        impure_status = "HARAM"
    elif impure_val >= 4.0 and biz_status == "DOUBTFUL":
        impure_status = "DOUBTFUL"
    else:
        impure_status = "HALAL"

    statuses = [biz_status, debt_status, dep_status, impure_status]
    haram_count = statuses.count("HARAM")
    doubtful_count = statuses.count("DOUBTFUL")
    passed_count = statuses.count("HALAL")

    if haram_count > 0:
        overall = "HARAM"
        verdict_summary = f"Не соответствует {haram_count} из 4 критериев AAOIFI"
    elif doubtful_count > 0:
        overall = "DOUBTFUL"
        verdict_summary = "Требует внимания: пограничные показатели деятельности"
    else:
        overall = "HALAL"
        verdict_summary = f"Соответствует {passed_count} из 4 критериев AAOIFI"

    # Purification applies to permissible / doubtful dividend holdings
    if overall == "HARAM" or data.dividend_per_quarter <= 0 or impure_val <= 0:
        purification = 0.0
    else:
        purification = round(float(data.dividend_per_quarter) * (impure_val / 100.0), 4)

    biz_pct = 100.0 if biz_status != "DOUBTFUL" else 75.0

    criteria = [
        {
            "id": "business",
            "title": "Деятельность",
            "title_uz": "Faoliyat",
            "value_label": biz_label,
            "subtitle": biz_sub_ru,
            "subtitle_uz": biz_sub_uz,
            "pct": biz_pct,
            "limit_pct": 100.0,
            "status": biz_status,
        },
        {
            "id": "debt",
            "title": "Долг",
            "title_uz": "Qarz yuki",
            "value_label": _fmt_pct(debt_val),
            "subtitle": "лимит 30%",
            "subtitle_uz": "limit 30%",
            "pct": debt_val,
            "limit_pct": AAOIFI_DEBT_LIMIT_PCT,
            "status": debt_status,
        },
        {
            "id": "deposits",
            "title": "Депозиты",
            "title_uz": "Foizli depozitlar",
            "value_label": _fmt_pct(dep_val),
            "subtitle": "лимит 30%",
            "subtitle_uz": "limit 30%",
            "pct": dep_val,
            "limit_pct": AAOIFI_INTEREST_DEPOSIT_LIMIT_PCT,
            "status": dep_status,
        },
        {
            "id": "impure",
            "title": "Недоп. доход",
            "title_uz": "Nomaqbul daromad",
            "value_label": _fmt_pct(impure_val),
            "subtitle": "лимит 5%",
            "subtitle_uz": "limit 5%",
            "pct": impure_val,
            "limit_pct": AAOIFI_IMPURE_REVENUE_LIMIT_PCT,
            "status": impure_status,
        },
    ]

    return AaoifiScreeningResult(
        halal_status=overall,
        business_status=biz_status,
        purification_per_dividend=purification,
        verdict_summary=verdict_summary,
        verdict_subtitle="Методика AAOIFI / WeGrow, пересмотр раз в квартал",
        criteria=criteria,
    )

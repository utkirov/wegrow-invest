from app.services.halal_screener import AaoifiScreeningInput, screen_stock_aaoifi


def test_aaoifi_all_criteria_pass():
    res = screen_stock_aaoifi(
        AaoifiScreeningInput(
            industry="Semiconductors",
            debt_ratio_pct=12.4,
            interest_deposit_ratio_pct=8.1,
            impure_revenue_pct=1.2,
            dividend_per_quarter=0.50,
        )
    )
    assert res.halal_status == "HALAL"
    assert res.business_status == "HALAL"
    assert res.purification_per_dividend == 0.006
    assert len(res.criteria) == 4
    assert all(c["status"] == "HALAL" for c in res.criteria)


def test_aaoifi_debt_boundary_29_9_vs_30_0():
    pass_res = screen_stock_aaoifi(
        AaoifiScreeningInput(
            industry="Software - Infrastructure",
            debt_ratio_pct=29.9,
            interest_deposit_ratio_pct=10.0,
            impure_revenue_pct=1.0,
        )
    )
    fail_res = screen_stock_aaoifi(
        AaoifiScreeningInput(
            industry="Software - Infrastructure",
            debt_ratio_pct=30.0,
            interest_deposit_ratio_pct=10.0,
            impure_revenue_pct=1.0,
        )
    )
    assert pass_res.halal_status == "HALAL"
    assert fail_res.halal_status == "HARAM"


def test_aaoifi_deposit_boundary_29_9_vs_30_0():
    pass_res = screen_stock_aaoifi(
        AaoifiScreeningInput(
            industry="Medical Devices",
            debt_ratio_pct=5.0,
            interest_deposit_ratio_pct=29.9,
            impure_revenue_pct=0.5,
        )
    )
    fail_res = screen_stock_aaoifi(
        AaoifiScreeningInput(
            industry="Medical Devices",
            debt_ratio_pct=5.0,
            interest_deposit_ratio_pct=30.0,
            impure_revenue_pct=0.5,
        )
    )
    assert pass_res.halal_status == "HALAL"
    assert fail_res.halal_status == "HARAM"


def test_aaoifi_impure_revenue_boundary_4_9_vs_5_0():
    pass_res = screen_stock_aaoifi(
        AaoifiScreeningInput(
            industry="Consumer Electronics",
            debt_ratio_pct=8.0,
            interest_deposit_ratio_pct=5.0,
            impure_revenue_pct=4.9,
            dividend_per_quarter=1.0,
        )
    )
    fail_res = screen_stock_aaoifi(
        AaoifiScreeningInput(
            industry="Consumer Electronics",
            debt_ratio_pct=8.0,
            interest_deposit_ratio_pct=5.0,
            impure_revenue_pct=5.0,
            dividend_per_quarter=1.0,
        )
    )
    assert pass_res.halal_status == "HALAL"
    assert pass_res.purification_per_dividend == 0.049
    assert fail_res.halal_status == "HARAM"
    assert fail_res.purification_per_dividend == 0.0


def test_aaoifi_haram_industry_and_doubtful_override():
    bank_res = screen_stock_aaoifi(
        AaoifiScreeningInput(
            industry="Conventional Banking",
            debt_ratio_pct=10.0,
            interest_deposit_ratio_pct=10.0,
            impure_revenue_pct=1.0,
            dividend_per_quarter=1.25,
        )
    )
    assert bank_res.halal_status == "HARAM"
    assert bank_res.business_status == "HARAM"
    assert bank_res.purification_per_dividend == 0.0

    doubtful_res = screen_stock_aaoifi(
        AaoifiScreeningInput(
            industry="Mixed Retail & Media",
            debt_ratio_pct=14.0,
            interest_deposit_ratio_pct=8.0,
            impure_revenue_pct=4.2,
            business_override="DOUBTFUL",
        )
    )
    assert doubtful_res.halal_status == "DOUBTFUL"

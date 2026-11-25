def test_seeded_universe_has_100_plus_stocks_and_pagination(client):
    health = client.get("/api/v1/health")
    assert health.status_code == 200
    assert health.json()["stocks_indexed"] >= 100

    page1 = client.get("/api/v1/stocks", params={"page": 1, "page_size": 20})
    assert page1.status_code == 200
    body = page1.json()
    assert len(body["stocks"]) == 20
    assert body["pagination"]["total"] >= 100
    assert body["pagination"]["total_pages"] >= 5
    assert len(body["sectors"]) >= 7


def test_stock_filtering_search_and_halal_statuses(client):
    halal = client.get("/api/v1/stocks", params={"halal_status": "HALAL", "page_size": 200})
    assert halal.status_code == 200
    halal_items = halal.json()["stocks"]
    assert len(halal_items) >= 70
    assert all(s["halal_status"] == "HALAL" for s in halal_items)

    haram = client.get("/api/v1/stocks", params={"halal_status": "HARAM", "page_size": 50})
    assert haram.status_code == 200
    haram_symbols = {s["symbol"] for s in haram.json()["stocks"]}
    assert "JPM" in haram_symbols
    assert "PM" in haram_symbols

    search = client.get("/api/v1/stocks", params={"q": "nvidia"})
    assert search.status_code == 200
    assert any(s["symbol"] == "NVDA" for s in search.json()["stocks"])


def test_multi_timeframe_charts_and_market_overview(client):
    for tf in ("1D", "1W", "1M", "6M", "1Y"):
        resp = client.get("/api/v1/stocks/AAPL/chart", params={"timeframe": tf})
        assert resp.status_code == 200
        data = resp.json()
        assert data["symbol"] == "AAPL"
        assert data["timeframe"] == tf
        assert len(data["points"]) >= 10

    bad = client.get("/api/v1/stocks/AAPL/chart", params={"timeframe": "99Y"})
    assert bad.status_code == 422

    overview = client.get("/api/v1/market/overview")
    assert overview.status_code == 200
    ov = overview.json()
    assert ov["total_stocks"] >= 100
    assert ov["halal_stocks"] >= 70
    assert len(ov["indices"]) == 3
    assert len(ov["heatmap"]) >= 6


def test_legacy_bootstrap_compatibility(client):
    resp = client.get("/api/bootstrap")
    assert resp.status_code == 200
    data = resp.json()
    assert "profile" in data
    assert "market_overview" in data
    assert len(data["stocks"]) >= 100
    assert len(data["goals"]) == 3
    assert len(data["courses"]) == 1

def _auth_headers(client, email: str) -> dict:
    req = client.post("/api/v1/auth/otp/request", json={"identifier": email}).json()
    ver = client.post(
        "/api/v1/auth/otp/verify",
        json={"identifier": email, "code": req["dev_code"]},
    ).json()
    return {"Authorization": f"Bearer {ver['access_token']}"}


def test_portfolio_weighted_avg_sell_and_halal_warnings(client):
    headers = _auth_headers(client, "portfolio1@wegrow.uz")

    # Buy 2 lots of AAPL (10 @ 200 and 10 @ 220 => avg 210, qty 20)
    r1 = client.post(
        "/api/v1/portfolio/transactions",
        headers=headers,
        json={"symbol": "AAPL", "side": "BUY", "quantity": 10, "price": 200.0},
    )
    assert r1.status_code == 200
    r2 = client.post(
        "/api/v1/portfolio/transactions",
        headers=headers,
        json={"symbol": "AAPL", "side": "BUY", "quantity": 10, "price": 220.0},
    )
    assert r2.status_code == 200

    # Partial sell 5 @ 230 => qty 15, realized pnl = 5 * (230 - 210) = 100
    r3 = client.post(
        "/api/v1/portfolio/transactions",
        headers=headers,
        json={"symbol": "AAPL", "side": "SELL", "quantity": 5, "price": 230.0},
    )
    assert r3.status_code == 200
    portfolio = r3.json()["portfolio"]
    assert portfolio["summary"]["realized_pnl_abs"] == 100.0
    aapl = portfolio["positions"][0]
    assert aapl["symbol"] == "AAPL"
    assert aapl["quantity"] == 15.0
    assert aapl["avg_price"] == 210.0
    assert portfolio["summary"]["halal_compliance_pct"] == 100.0

    # Oversell must fail with 400
    oversell = client.post(
        "/api/v1/portfolio/transactions",
        headers=headers,
        json={"symbol": "AAPL", "side": "SELL", "quantity": 50, "price": 230.0},
    )
    assert oversell.status_code == 400

    # Buying a HARAM stock (JPM) reduces halal compliance % and emits warning
    r4 = client.post(
        "/api/v1/portfolio/transactions",
        headers=headers,
        json={"symbol": "JPM", "side": "BUY", "quantity": 5, "price": 230.0},
    )
    assert r4.status_code == 200
    p2 = r4.json()["portfolio"]
    assert p2["summary"]["halal_compliance_pct"] < 100.0
    assert any(w["symbol"] == "JPM" for w in p2["non_halal_warnings"])


def test_watchlist_alerts_and_purification_ledger(client):
    headers = _auth_headers(client, "portfolio2@wegrow.uz")

    # Watchlist add / get / remove
    assert client.post("/api/v1/watchlist/NVDA", headers=headers).status_code == 200
    assert client.post("/api/v1/watchlist/MSFT", headers=headers).status_code == 200
    wl = client.get("/api/v1/watchlist", headers=headers).json()
    assert set(wl["symbols"]) == {"NVDA", "MSFT"}
    assert client.delete("/api/v1/watchlist/NVDA", headers=headers).status_code == 200
    wl2 = client.get("/api/v1/watchlist", headers=headers).json()
    assert wl2["symbols"] == ["MSFT"]

    # Price alerts: AAPL current seeded price is 228.45 -> ABOVE 200 triggers immediately
    a1 = client.post(
        "/api/v1/alerts",
        headers=headers,
        json={"symbol": "AAPL", "condition": "ABOVE", "target_price": 200.0},
    )
    assert a1.status_code == 200
    assert a1.json()["alert"]["triggered"] is True

    # BELOW 100 stays active & untriggered
    a2 = client.post(
        "/api/v1/alerts",
        headers=headers,
        json={"symbol": "AAPL", "condition": "BELOW", "target_price": 100.0},
    )
    assert a2.status_code == 200
    assert a2.json()["alert"]["triggered"] is False
    assert a2.json()["alert"]["active"] is True

    # Purification ledger: buy 100 shares of MSFT (pays quarterly dividend & has impure > 0)
    assert (
        client.post(
            "/api/v1/portfolio/transactions",
            headers=headers,
            json={"symbol": "MSFT", "side": "BUY", "quantity": 100, "price": 410.0},
        ).status_code
        == 200
    )
    ledger = client.get("/api/v1/purification", headers=headers).json()
    assert ledger["total_pending_usd"] > 0
    msft_item = next(i for i in ledger["items"] if i["symbol"] == "MSFT")
    assert msft_item["status"] == "PENDING"

    # Mark MSFT purification cleared
    cleared = client.post(
        "/api/v1/purification/clear",
        headers=headers,
        json={
            "symbol": "MSFT",
            "amount": msft_item["remaining_amount"],
            "quarter_label": ledger["quarter_label"],
            "shares": 100,
            "dividend_total": msft_item["dividend_total"],
        },
    )
    assert cleared.status_code == 200
    after = cleared.json()["ledger"]
    assert after["total_pending_usd"] == 0.0
    assert after["total_cleared_usd"] == msft_item["purification_due"]

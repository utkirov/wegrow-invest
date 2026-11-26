def test_news_list_filter_and_detail(client):
    res = client.get("/api/v1/news")
    assert res.status_code == 200
    data = res.json()
    assert data["total"] >= 6
    first = data["news"][0]
    assert first["summary"]
    assert first["body"]
    assert first["read_min"] >= 1

    filtered = client.get("/api/v1/news?ticker=NVDA")
    assert filtered.status_code == 200
    assert all(item["ticker"] == "NVDA" for item in filtered.json()["news"])

    detail = client.get(f"/api/v1/news/{first['id']}")
    assert detail.status_code == 200
    assert detail.json()["article"]["id"] == first["id"]

    not_found = client.get("/api/v1/news/999999")
    assert not_found.status_code == 404


def test_stories_brokers_and_streams_endpoints(client):
    stories_res = client.get("/api/v1/stories")
    assert stories_res.status_code == 200
    stories = stories_res.json()["stories"]
    assert len(stories) >= 5
    assert all(len(deck["slides"]) >= 1 for deck in stories)

    brokers_res = client.get("/api/v1/brokers?us_stocks=true")
    assert brokers_res.status_code == 200
    brokers = brokers_res.json()["brokers"]
    assert len(brokers) >= 2
    assert all(b["us_stocks"] is True for b in brokers)

    streams_res = client.get("/api/v1/streams?status=UPCOMING")
    assert streams_res.status_code == 200
    streams = streams_res.json()["streams"]
    assert len(streams) >= 1
    assert all(s["status"] == "UPCOMING" for s in streams)


def test_user_orders_history_endpoint(client):
    # Create an order and verify GET /api/v1/orders returns it with course title
    create_res = client.post("/api/v1/orders", json={"course_id": 1, "provider": "PAYME"})
    assert create_res.status_code == 200
    uid = create_res.json()["order"]["order_uid"]

    list_res = client.get("/api/v1/orders")
    assert list_res.status_code == 200
    orders = list_res.json()["orders"]
    assert any(o["order_uid"] == uid and o["status"] == "PENDING" and o["course_title"] for o in orders)

    pay_res = client.post(f"/api/v1/orders/{uid}/sandbox-pay")
    assert pay_res.status_code == 200

    list_after = client.get("/api/v1/orders")
    assert list_after.status_code == 200
    paid_order = next(o for o in list_after.json()["orders"] if o["order_uid"] == uid)
    assert paid_order["status"] == "PAID"

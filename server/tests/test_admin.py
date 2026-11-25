from app.core.config import get_settings


def _admin_headers() -> dict:
    return {"X-Admin-Key": get_settings().admin_api_key}


def test_admin_auth_and_ui(client):
    assert client.get("/api/v1/admin/stats").status_code == 401
    assert client.get("/api/v1/admin/stats", headers={"X-Admin-Key": "wrong"}).status_code == 401

    stats = client.get("/api/v1/admin/stats", headers=_admin_headers())
    assert stats.status_code == 200
    body = stats.json()
    assert body["stocks_total"] >= 100
    assert body["stocks_halal"] >= 70
    assert body["lessons_total"] >= 5

    ui = client.get("/admin")
    assert ui.status_code == 200
    assert "WeGrow Admin CMS" in ui.text


def test_admin_halal_audit_override_recomputes_aaoifi(client):
    # Override AAPL with debt 35% -> AAOIFI engine must flip AAPL to HARAM
    upd = client.put(
        "/api/v1/admin/stocks/AAPL/halal",
        headers=_admin_headers(),
        json={
            "debt_ratio_pct": 35.0,
            "interest_deposit_ratio_pct": 5.0,
            "impure_revenue_pct": 1.0,
            "verdict_note": "Временное превышение долга > 30% по аудиту",
        },
    )
    assert upd.status_code == 200
    stock = upd.json()["stock"]
    assert stock["halal_status"] == "HARAM"
    assert stock["verdict_summary"] == "Временное превышение долга > 30% по аудиту"

    # Confirm public stock endpoint reflects the new audit verdict immediately
    public = client.get("/api/v1/stocks/AAPL").json()
    assert public["halal_status"] == "HARAM"


def test_admin_lms_lesson_crud_and_orders(client):
    created = client.post(
        "/api/v1/admin/lessons",
        headers=_admin_headers(),
        json={
            "course_id": 1,
            "module_number": 5,
            "lesson_number": 2,
            "title": "Налоги с дивидендов США (форма W-8BEN)",
            "title_uz": "AQSh dividend soliqlari (W-8BEN shakli)",
            "lesson_type": "article",
            "duration_min": 12,
            "content_md": "## Форма W-8BEN\nСнижает налог на дивиденды США.",
            "is_preview": True,
        },
    )
    assert created.status_code == 200
    lesson_id = created.json()["lesson_id"]

    lessons = client.get("/api/v1/admin/lessons", headers=_admin_headers()).json()["lessons"]
    assert any(l["id"] == lesson_id for l in lessons)

    deleted = client.delete(f"/api/v1/admin/lessons/{lesson_id}", headers=_admin_headers())
    assert deleted.status_code == 200

    orders = client.get("/api/v1/admin/orders", headers=_admin_headers())
    assert orders.status_code == 200
    assert "orders" in orders.json()

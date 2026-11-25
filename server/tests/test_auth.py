def _login_with_otp(client, identifier: str, first_name: str = "Тест") -> dict:
    req = client.post("/api/v1/auth/otp/request", json={"identifier": identifier})
    assert req.status_code == 200
    body = req.json()
    assert body["status"] == "sent"
    assert "dev_code" in body

    ver = client.post(
        "/api/v1/auth/otp/verify",
        json={"identifier": identifier, "code": body["dev_code"], "first_name": first_name},
    )
    assert ver.status_code == 200
    return ver.json()


def test_phone_otp_login_refresh_and_logout(client):
    tokens = _login_with_otp(client, "+998 90 555-44-33", first_name="Сардор")
    access = tokens["access_token"]
    refresh = tokens["refresh_token"]
    assert tokens["user"]["phone"] == "+998905554433"
    assert tokens["user"]["first_name"] == "Сардор"

    me = client.get("/api/v1/auth/me", headers={"Authorization": f"Bearer {access}"})
    assert me.status_code == 200
    assert me.json()["user"]["phone"] == "+998905554433"

    # Refresh token rotation
    ref = client.post("/api/v1/auth/refresh", json={"refresh_token": refresh})
    assert ref.status_code == 200
    new_refresh = ref.json()["refresh_token"]
    assert new_refresh != refresh

    # Old refresh token must be rejected after rotation
    reused = client.post("/api/v1/auth/refresh", json={"refresh_token": refresh})
    assert reused.status_code == 401

    # Logout revokes current refresh token
    out = client.post("/api/v1/auth/logout", json={"refresh_token": new_refresh})
    assert out.status_code == 200
    after_logout = client.post("/api/v1/auth/refresh", json={"refresh_token": new_refresh})
    assert after_logout.status_code == 401


def test_invalid_otp_is_rejected(client):
    req = client.post("/api/v1/auth/otp/request", json={"identifier": "investor@wegrow.uz"})
    assert req.status_code == 200
    wrong = client.post(
        "/api/v1/auth/otp/verify",
        json={"identifier": "investor@wegrow.uz", "code": "000000"},
    )
    assert wrong.status_code == 401


def test_guest_login_and_user_goal_isolation(client):
    user_a = _login_with_otp(client, "user_a@wegrow.uz", first_name="Алишер")
    user_b = client.post("/api/v1/auth/guest").json()

    headers_a = {"Authorization": f"Bearer {user_a['access_token']}"}
    headers_b = {"Authorization": f"Bearer {user_b['access_token']}"}

    created = client.post(
        "/api/v1/goals",
        headers=headers_a,
        json={"title": "Личная цель Алишера", "target_amount": 5000.0, "monthly_contribution": 400.0},
    )
    assert created.status_code == 200
    goal_id = created.json()["goal"]["id"]

    # User B must not see or modify User A's goal
    goals_b = client.get("/api/v1/goals", headers=headers_b).json()["goals"]
    assert all(g["id"] != goal_id for g in goals_b)

    forbidden_contribute = client.post(
        f"/api/v1/goals/{goal_id}/contribute",
        headers=headers_b,
        json={"amount": 100.0},
    )
    assert forbidden_contribute.status_code == 404

    # User A can contribute and delete their own goal
    ok_contribute = client.post(
        f"/api/v1/goals/{goal_id}/contribute",
        headers=headers_a,
        json={"amount": 250.0},
    )
    assert ok_contribute.status_code == 200
    assert ok_contribute.json()["goal"]["current_amount"] == 250.0

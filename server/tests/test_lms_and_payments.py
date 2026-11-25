import base64

from app.core.config import get_settings
from app.payments.click import compute_click_signature


def _auth_headers(client, email: str) -> dict:
    req = client.post("/api/v1/auth/otp/request", json={"identifier": email}).json()
    ver = client.post(
        "/api/v1/auth/otp/verify",
        json={"identifier": email, "code": req["dev_code"]},
    ).json()
    return {"Authorization": f"Bearer {ver['access_token']}"}


def _payme_auth_header(key: str | None = None) -> dict:
    secret = key if key is not None else get_settings().payme_merchant_key
    token = base64.b64encode(f"Paycom:{secret}".encode("utf-8")).decode("ascii")
    return {"Authorization": f"Basic {token}"}


def test_lms_paywall_preview_and_quiz_grading(client):
    headers = _auth_headers(client, "student1@wegrow.uz")

    course = client.get("/api/v1/courses/1", headers=headers).json()
    assert course["purchased"] is False
    lessons = course["lessons"]
    assert len(lessons) >= 5

    preview_lesson = next(l for l in lessons if l["is_preview"])
    paid_lesson = next(l for l in lessons if not l["is_preview"])

    # Free preview is accessible without purchase
    prev_detail = client.get(f"/api/v1/courses/1/lessons/{preview_lesson['id']}", headers=headers)
    assert prev_detail.status_code == 200
    assert prev_detail.json()["content_md"]

    # Paid lesson returns 403 before purchase
    locked_resp = client.get(f"/api/v1/courses/1/lessons/{paid_lesson['id']}", headers=headers)
    assert locked_resp.status_code == 403

    # Complete preview lesson + quiz grading
    prog = client.post(
        f"/api/v1/courses/1/lessons/{preview_lesson['id']}/progress",
        headers=headers,
        json={"completed": True, "position_sec": 720, "quiz_answers": {"q1": 0}},
    )
    assert prog.status_code == 200
    assert prog.json()["quiz_score"] == 100
    assert prog.json()["course_progress"]["completed_lessons"] == 1


def test_payme_json_rpc_full_lifecycle_and_errors(client):
    headers = _auth_headers(client, "payme_buyer@wegrow.uz")

    order_resp = client.post("/api/v1/orders", headers=headers, json={"course_id": 1, "provider": "PAYME"})
    assert order_resp.status_code == 200
    order = order_resp.json()["order"]
    order_uid = order["order_uid"]
    amount_tiyin = order["amount_tiyin"]
    assert "checkout.paycom.uz" in order["checkout_url"]

    # 1. Invalid Basic Auth -> -32504
    unauth = client.post(
        "/api/v1/payments/payme/rpc",
        headers=_payme_auth_header("wrong_key"),
        json={"id": 1, "method": "CheckPerformTransaction", "params": {"amount": amount_tiyin, "account": {"order_id": order_uid}}},
    )
    assert unauth.json()["error"]["code"] == -32504

    # 2. Wrong amount -> -31001
    wrong_amt = client.post(
        "/api/v1/payments/payme/rpc",
        headers=_payme_auth_header(),
        json={"id": 2, "method": "CheckPerformTransaction", "params": {"amount": 999, "account": {"order_id": order_uid}}},
    )
    assert wrong_amt.json()["error"]["code"] == -31001

    # 3. CheckPerformTransaction -> allow: true
    ok_check = client.post(
        "/api/v1/payments/payme/rpc",
        headers=_payme_auth_header(),
        json={"id": 3, "method": "CheckPerformTransaction", "params": {"amount": amount_tiyin, "account": {"order_id": order_uid}}},
    )
    assert ok_check.json()["result"]["allow"] is True

    # 4. CreateTransaction -> state 1
    tx_id = "payme_tx_90001"
    created = client.post(
        "/api/v1/payments/payme/rpc",
        headers=_payme_auth_header(),
        json={"id": 4, "method": "CreateTransaction", "params": {"id": tx_id, "time": 1700000000000, "amount": amount_tiyin, "account": {"order_id": order_uid}}},
    )
    assert created.json()["result"]["state"] == 1

    # 5. PerformTransaction -> state 2 + unlocks course
    performed = client.post(
        "/api/v1/payments/payme/rpc",
        headers=_payme_auth_header(),
        json={"id": 5, "method": "PerformTransaction", "params": {"id": tx_id}},
    )
    assert performed.json()["result"]["state"] == 2

    # Idempotent PerformTransaction returns state 2 again
    performed_again = client.post(
        "/api/v1/payments/payme/rpc",
        headers=_payme_auth_header(),
        json={"id": 6, "method": "PerformTransaction", "params": {"id": tx_id}},
    )
    assert performed_again.json()["result"]["state"] == 2

    # Verify paid lesson is now unlocked (200 OK)
    course = client.get("/api/v1/courses/1", headers=headers).json()
    assert course["purchased"] is True
    paid_lesson = next(l for l in course["lessons"] if not l["is_preview"])
    assert client.get(f"/api/v1/courses/1/lessons/{paid_lesson['id']}", headers=headers).status_code == 200


def test_click_prepare_complete_signature_and_unlock(client):
    headers = _auth_headers(client, "click_buyer@wegrow.uz")

    order = client.post("/api/v1/orders", headers=headers, json={"course_id": 1, "provider": "CLICK"}).json()["order"]
    order_uid = order["order_uid"]
    amount = str(order["amount_uzs"])
    service_id = str(get_settings().click_service_id)
    sign_time = "2026-11-25 15:30:00"

    # Bad signature rejected with error -1
    bad_sig = client.post(
        "/api/v1/payments/click/prepare",
        json={
            "click_trans_id": "555001",
            "service_id": service_id,
            "merchant_trans_id": order_uid,
            "amount": amount,
            "action": "0",
            "sign_time": sign_time,
            "sign_string": "deadbeef",
        },
    )
    assert bad_sig.json()["error"] == -1

    # Valid Prepare
    prep_sig = compute_click_signature("555001", service_id, order_uid, amount, "0", sign_time)
    prep = client.post(
        "/api/v1/payments/click/prepare",
        json={
            "click_trans_id": "555001",
            "service_id": service_id,
            "merchant_trans_id": order_uid,
            "amount": amount,
            "action": "0",
            "sign_time": sign_time,
            "sign_string": prep_sig,
        },
    ).json()
    assert prep["error"] == 0
    prepare_id = str(prep["merchant_prepare_id"])

    # Valid Complete
    comp_sig = compute_click_signature(
        "555001", service_id, order_uid, amount, "1", sign_time, merchant_prepare_id=prepare_id
    )
    comp = client.post(
        "/api/v1/payments/click/complete",
        json={
            "click_trans_id": "555001",
            "service_id": service_id,
            "merchant_trans_id": order_uid,
            "merchant_prepare_id": prepare_id,
            "amount": amount,
            "action": "1",
            "error": 0,
            "sign_time": sign_time,
            "sign_string": comp_sig,
        },
    ).json()
    assert comp["error"] == 0

    # Duplicate Complete returns -4 (Already paid)
    dup = client.post(
        "/api/v1/payments/click/complete",
        json={
            "click_trans_id": "555001",
            "service_id": service_id,
            "merchant_trans_id": order_uid,
            "merchant_prepare_id": prepare_id,
            "amount": amount,
            "action": "1",
            "error": 0,
            "sign_time": sign_time,
            "sign_string": comp_sig,
        },
    ).json()
    assert dup["error"] == -4

    # Course is now unlocked for this user
    course = client.get("/api/v1/courses/1", headers=headers).json()
    assert course["purchased"] is True

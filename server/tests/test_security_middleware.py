def test_security_headers_and_request_id(client):
    resp = client.get("/api/v1/health")
    assert resp.status_code == 200
    assert resp.headers.get("X-Content-Type-Options") == "nosniff"
    assert resp.headers.get("X-Frame-Options") == "DENY"
    assert resp.headers.get("X-Request-ID")
    assert resp.headers.get("X-Process-Time-Ms")


def test_otp_rate_limit_blocks_excessive_requests(client):
    # Middleware default OTP bucket allows 10 requests/min per IP
    statuses = []
    for idx in range(12):
        r = client.post("/api/v1/auth/otp/request", json={"identifier": f"spam{idx}@wegrow.uz"})
        statuses.append(r.status_code)
    assert 429 in statuses

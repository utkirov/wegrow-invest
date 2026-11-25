import time
import uuid
from collections import defaultdict, deque
from typing import Deque, Dict, Tuple

from starlette.middleware.base import BaseHTTPMiddleware
from starlette.requests import Request
from starlette.responses import JSONResponse, Response

from app.core.config import get_settings


class SecurityAndRateLimitMiddleware(BaseHTTPMiddleware):
    """Adds security headers, request timing/ID, and sliding-window rate limiting."""

    def __init__(self, app, otp_limit_per_min: int = 10, default_limit_per_min: int = 180):
        super().__init__(app)
        self.otp_limit = otp_limit_per_min
        self.default_limit = default_limit_per_min
        self._buckets: Dict[Tuple[str, str], Deque[float]] = defaultdict(deque)

    def _is_rate_limited(self, client_ip: str, bucket: str, limit: int) -> bool:
        now = time.monotonic()
        window_start = now - 60.0
        q = self._buckets[(client_ip, bucket)]
        while q and q[0] < window_start:
            q.popleft()
        if len(q) >= limit:
            return True
        q.append(now)
        return False

    async def dispatch(self, request: Request, call_next) -> Response:
        req_id = request.headers.get("X-Request-ID") or uuid.uuid4().hex[:12]
        start = time.perf_counter()

        settings = get_settings()
        if settings.environment != "test":
            client_ip = request.client.host if request.client else "unknown"
            path = request.url.path
            if path.startswith("/api/v1/auth/otp"):
                if self._is_rate_limited(client_ip, "otp", self.otp_limit):
                    return JSONResponse(
                        status_code=429,
                        content={"detail": "Too many OTP requests, please retry later"},
                        headers={"X-Request-ID": req_id},
                    )
            elif path.startswith("/api/"):
                if self._is_rate_limited(client_ip, "api", self.default_limit):
                    return JSONResponse(
                        status_code=429,
                        content={"detail": "Rate limit exceeded"},
                        headers={"X-Request-ID": req_id},
                    )

        response = await call_next(request)
        elapsed_ms = round((time.perf_counter() - start) * 1000.0, 2)
        response.headers["X-Request-ID"] = req_id
        response.headers["X-Process-Time-Ms"] = str(elapsed_ms)
        response.headers["X-Content-Type-Options"] = "nosniff"
        if request.url.path.startswith("/api/") or settings.environment == "production":
            response.headers["X-Frame-Options"] = "DENY"
        return response

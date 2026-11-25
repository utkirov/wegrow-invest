import os
from functools import lru_cache
from typing import List

from pydantic_settings import BaseSettings, SettingsConfigDict

SERVER_DIR = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
DEFAULT_SQLITE_PATH = os.path.join(SERVER_DIR, "wegrow.db")


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_prefix="WEGROW_",
        env_file=os.path.join(SERVER_DIR, ".env"),
        env_file_encoding="utf-8",
        extra="ignore",
    )

    app_name: str = "WeGrow Invest API v4.0"
    app_version: str = "4.0.0"
    environment: str = "development"

    database_url: str = f"sqlite:///{DEFAULT_SQLITE_PATH}"

    jwt_secret: str = "dev-wegrow-jwt-secret-change-in-production-32bytes"
    jwt_algorithm: str = "HS256"
    jwt_access_ttl_minutes: int = 60
    jwt_refresh_ttl_days: int = 30

    otp_ttl_seconds: int = 300
    otp_max_attempts: int = 5
    otp_dev_echo: bool = True

    market_provider: str = "yfinance"
    market_live_enabled: bool = True
    market_cache_ttl_seconds: int = 900
    market_request_timeout_seconds: float = 5.0

    # Payment gateways (Uzbekistan: Payme Merchant API & Click SHOP API)
    payment_sandbox: bool = True
    payme_merchant_id: str = "dev_payme_merchant_id"
    payme_merchant_key: str = "dev_payme_secret_key"
    payme_checkout_url: str = "https://checkout.paycom.uz"

    click_service_id: int = 10001
    click_merchant_id: int = 20001
    click_secret_key: str = "dev_click_secret_key"
    click_checkout_url: str = "https://my.click.uz/services/pay"

    allowed_origins: str = "*"

    @property
    def cors_origins(self) -> List[str]:
        raw = self.allowed_origins.strip()
        if raw == "*":
            return ["*"]
        return [origin.strip() for origin in raw.split(",") if origin.strip()]


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    return Settings()

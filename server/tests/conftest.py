import os
import sys

import pytest
from fastapi.testclient import TestClient

SERVER_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
if SERVER_DIR not in sys.path:
    sys.path.insert(0, SERVER_DIR)

from app.core.config import get_settings  # noqa: E402
from app.db.seed import seed_all  # noqa: E402
from app.db.session import reconfigure_database  # noqa: E402
from app.main import create_app  # noqa: E402


@pytest.fixture()
def client(tmp_path, monkeypatch):
    db_file = tmp_path / "test_wegrow.db"
    monkeypatch.setenv("WEGROW_DATABASE_URL", f"sqlite:///{db_file}")
    monkeypatch.setenv("WEGROW_MARKET_LIVE_ENABLED", "false")
    monkeypatch.setenv("WEGROW_OTP_DEV_ECHO", "true")
    get_settings.cache_clear()

    reconfigure_database(f"sqlite:///{db_file}")
    seed_all()

    app = create_app()
    with TestClient(app) as test_client:
        yield test_client

    get_settings.cache_clear()

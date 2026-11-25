"""Entrypoint for WeGrow Invest API v4.0.

Preserves direct execution (`python3 server/main.py`) and ASGI import (`server.main:app`).
"""

import os
import sys

SERVER_DIR = os.path.dirname(os.path.abspath(__file__))
if SERVER_DIR not in sys.path:
    sys.path.insert(0, SERVER_DIR)

from app.db.seed import seed_all  # noqa: E402
from app.main import app, create_app  # noqa: E402

seed_all()

if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=8090)

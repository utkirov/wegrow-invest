from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.core.config import get_settings
from app.db.seed import seed_all
from app.routers import auth, core_api, market


@asynccontextmanager
async def lifespan(_app: FastAPI):
    seed_all()
    yield


def create_app() -> FastAPI:
    settings = get_settings()
    application = FastAPI(
        title=settings.app_name,
        description=(
            "Production-ready Backend API for WeGrow Invest v4.0 "
            "(OTP/JWT Auth, 100+ US Equities, AAOIFI Sharia Screening, Courses & Goals)"
        ),
        version=settings.app_version,
        lifespan=lifespan,
    )

    application.add_middleware(
        CORSMiddleware,
        allow_origins=settings.cors_origins,
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )

    application.include_router(auth.router)
    application.include_router(market.router)
    application.include_router(core_api.router)
    return application


app = create_app()

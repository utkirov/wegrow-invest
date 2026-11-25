from datetime import datetime, timedelta, timezone
from typing import Optional

from fastapi import APIRouter, Depends, HTTPException, status
from pydantic import BaseModel, Field
from sqlalchemy.orm import Session

from app.core.config import get_settings
from app.core.security import (
    create_access_token,
    create_refresh_token,
    decode_token,
    generate_otp_code,
    get_current_user,
    hash_secret,
    utcnow,
    verify_hashed_secret,
)
from app.db.models import OtpCode, RefreshToken, User
from app.db.seed import seed_user_default_goals
from app.db.session import get_db
from app.services.otp_sender import get_otp_sender, normalize_identifier

router = APIRouter(prefix="/api/v1/auth", tags=["auth"])


class OtpRequestBody(BaseModel):
    identifier: str = Field(..., min_length=5, max_length=255, description="Phone (+998...) or email")


class OtpVerifyBody(BaseModel):
    identifier: str = Field(..., min_length=5, max_length=255)
    code: str = Field(..., min_length=4, max_length=10)
    first_name: Optional[str] = None
    last_name: Optional[str] = None
    language: Optional[str] = "ru"


class RefreshBody(BaseModel):
    refresh_token: str = Field(..., min_length=10)


class ProfileUpdateBody(BaseModel):
    first_name: str
    last_name: str
    middle_name: Optional[str] = ""
    birth_date: Optional[str] = "15.04.1996"
    gender: Optional[str] = "male"
    language: Optional[str] = "ru"
    dark_theme: Optional[bool] = True


def _issue_token_pair(db: Session, user: User) -> dict:
    access_token = create_access_token(user.id, is_guest=user.is_guest)
    refresh_token, expires_at = create_refresh_token(user.id)
    db.add(
        RefreshToken(
            user_id=user.id,
            token_hash=hash_secret(refresh_token),
            expires_at=expires_at,
            revoked=False,
        )
    )
    db.commit()
    return {
        "access_token": access_token,
        "refresh_token": refresh_token,
        "token_type": "bearer",
        "expires_in": get_settings().jwt_access_ttl_minutes * 60,
        "user": user.to_profile_dict(),
    }


@router.post("/otp/request")
def request_otp(body: OtpRequestBody, db: Session = Depends(get_db)):
    try:
        identifier, channel = normalize_identifier(body.identifier)
    except ValueError as exc:
        raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY, detail=str(exc)) from exc

    settings = get_settings()
    code = generate_otp_code(6)
    expires_at = utcnow() + timedelta(seconds=settings.otp_ttl_seconds)

    # Invalidate older unconsumed codes for the same identifier
    db.query(OtpCode).filter(
        OtpCode.identifier == identifier,
        OtpCode.consumed.is_(False),
    ).update({"consumed": True})

    otp = OtpCode(
        identifier=identifier,
        channel=channel,
        code_hash=hash_secret(code),
        expires_at=expires_at,
        attempts=0,
        consumed=False,
    )
    db.add(otp)
    db.commit()

    get_otp_sender().send(identifier, channel, code)

    response = {
        "status": "sent",
        "identifier": identifier,
        "channel": channel,
        "expires_in": settings.otp_ttl_seconds,
    }
    if settings.otp_dev_echo:
        response["dev_code"] = code
    return response


@router.post("/otp/verify")
def verify_otp(body: OtpVerifyBody, db: Session = Depends(get_db)):
    try:
        identifier, channel = normalize_identifier(body.identifier)
    except ValueError as exc:
        raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY, detail=str(exc)) from exc

    settings = get_settings()
    otp = (
        db.query(OtpCode)
        .filter(OtpCode.identifier == identifier, OtpCode.consumed.is_(False))
        .order_by(OtpCode.id.desc())
        .first()
    )
    if not otp:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="OTP code not found or already used")

    now = utcnow()
    otp_exp = otp.expires_at if otp.expires_at.tzinfo else otp.expires_at.replace(tzinfo=timezone.utc)
    if now > otp_exp:
        otp.consumed = True
        db.commit()
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="OTP code expired")

    if otp.attempts >= settings.otp_max_attempts:
        otp.consumed = True
        db.commit()
        raise HTTPException(status_code=status.HTTP_429_TOO_MANY_REQUESTS, detail="Too many invalid OTP attempts")

    if not verify_hashed_secret(body.code.strip(), otp.code_hash):
        otp.attempts += 1
        db.commit()
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid OTP code")

    otp.consumed = True
    db.flush()

    if channel == "phone":
        user = db.query(User).filter(User.phone == identifier).first()
    else:
        user = db.query(User).filter(User.email == identifier).first()

    if not user:
        user = User(
            phone=identifier if channel == "phone" else None,
            email=identifier if channel == "email" else None,
            first_name=body.first_name or "Инвестор",
            last_name=body.last_name or "WeGrow",
            middle_name="",
            language=body.language or "ru",
            dark_theme=True,
            is_guest=False,
        )
        db.add(user)
        db.flush()
        seed_user_default_goals(db, user.id)

    return _issue_token_pair(db, user)


@router.post("/guest")
def guest_login(db: Session = Depends(get_db)):
    user = User(
        first_name="Гость",
        last_name="WeGrow",
        middle_name="",
        language="ru",
        dark_theme=True,
        is_guest=True,
    )
    db.add(user)
    db.flush()
    seed_user_default_goals(db, user.id)
    return _issue_token_pair(db, user)


@router.post("/refresh")
def refresh_tokens(body: RefreshBody, db: Session = Depends(get_db)):
    payload = decode_token(body.refresh_token, expected_type="refresh")
    token_hash = hash_secret(body.refresh_token)
    stored = db.query(RefreshToken).filter(RefreshToken.token_hash == token_hash).first()
    if not stored or stored.revoked:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Refresh token revoked or invalid")

    exp = stored.expires_at if stored.expires_at.tzinfo else stored.expires_at.replace(tzinfo=timezone.utc)
    if utcnow() > exp:
        stored.revoked = True
        db.commit()
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Refresh token expired")

    user = db.get(User, int(payload["sub"]))
    if not user:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="User not found")

    # Rotate refresh token
    stored.revoked = True
    db.flush()
    return _issue_token_pair(db, user)


@router.post("/logout")
def logout(body: RefreshBody, db: Session = Depends(get_db)):
    token_hash = hash_secret(body.refresh_token)
    stored = db.query(RefreshToken).filter(RefreshToken.token_hash == token_hash).first()
    if stored:
        stored.revoked = True
        db.commit()
    return {"status": "ok"}


@router.get("/me")
def get_me(current_user: User = Depends(get_current_user)):
    return {"user": current_user.to_profile_dict()}


@router.put("/me")
def update_me(
    body: ProfileUpdateBody,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    current_user.first_name = body.first_name
    current_user.last_name = body.last_name
    current_user.middle_name = body.middle_name or ""
    current_user.birth_date = body.birth_date or current_user.birth_date
    current_user.gender = body.gender or current_user.gender
    current_user.language = body.language or current_user.language
    if body.dark_theme is not None:
        current_user.dark_theme = body.dark_theme
    db.add(current_user)
    db.commit()
    db.refresh(current_user)
    return {"status": "ok", "user": current_user.to_profile_dict()}

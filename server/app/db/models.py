from datetime import datetime, timezone
from typing import Optional

from sqlalchemy import (
    Boolean,
    DateTime,
    Float,
    ForeignKey,
    Integer,
    String,
    Text,
    UniqueConstraint,
)
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db.session import Base


def _utcnow() -> datetime:
    return datetime.now(timezone.utc)


class User(Base):
    __tablename__ = "users"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    phone: Mapped[Optional[str]] = mapped_column(String(32), unique=True, index=True, nullable=True)
    email: Mapped[Optional[str]] = mapped_column(String(255), unique=True, index=True, nullable=True)
    first_name: Mapped[str] = mapped_column(String(120), default="Азиз")
    last_name: Mapped[str] = mapped_column(String(120), default="Каримов")
    middle_name: Mapped[str] = mapped_column(String(120), default="")
    birth_date: Mapped[str] = mapped_column(String(32), default="15.04.1996")
    gender: Mapped[str] = mapped_column(String(16), default="male")
    language: Mapped[str] = mapped_column(String(8), default="ru")
    dark_theme: Mapped[bool] = mapped_column(Boolean, default=True)
    is_guest: Mapped[bool] = mapped_column(Boolean, default=False)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=_utcnow)

    goals: Mapped[list["Goal"]] = relationship("Goal", back_populates="user", cascade="all, delete-orphan")
    refresh_tokens: Mapped[list["RefreshToken"]] = relationship(
        "RefreshToken", back_populates="user", cascade="all, delete-orphan"
    )
    course_purchases: Mapped[list["UserCoursePurchase"]] = relationship(
        "UserCoursePurchase", back_populates="user", cascade="all, delete-orphan"
    )

    def to_profile_dict(self) -> dict:
        return {
            "id": self.id,
            "phone": self.phone,
            "email": self.email,
            "first_name": self.first_name,
            "last_name": self.last_name,
            "middle_name": self.middle_name,
            "birth_date": self.birth_date,
            "gender": self.gender,
            "language": self.language,
            "dark_theme": bool(self.dark_theme),
            "is_guest": bool(self.is_guest),
        }


class OtpCode(Base):
    __tablename__ = "otp_codes"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    identifier: Mapped[str] = mapped_column(String(255), index=True)
    channel: Mapped[str] = mapped_column(String(16), default="phone")  # phone | email
    code_hash: Mapped[str] = mapped_column(String(128))
    expires_at: Mapped[datetime] = mapped_column(DateTime(timezone=True))
    attempts: Mapped[int] = mapped_column(Integer, default=0)
    consumed: Mapped[bool] = mapped_column(Boolean, default=False)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=_utcnow)


class RefreshToken(Base):
    __tablename__ = "refresh_tokens"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"), index=True)
    token_hash: Mapped[str] = mapped_column(String(128), unique=True, index=True)
    expires_at: Mapped[datetime] = mapped_column(DateTime(timezone=True))
    revoked: Mapped[bool] = mapped_column(Boolean, default=False)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=_utcnow)

    user: Mapped["User"] = relationship("User", back_populates="refresh_tokens")


class Stock(Base):
    __tablename__ = "stocks"

    symbol: Mapped[str] = mapped_column(String(16), primary_key=True)
    name: Mapped[str] = mapped_column(String(255), index=True)
    exchange: Mapped[str] = mapped_column(String(32), default="NASDAQ")
    sector: Mapped[str] = mapped_column(String(120), index=True)
    sector_uz: Mapped[str] = mapped_column(String(120))
    industry: Mapped[str] = mapped_column(String(120), default="Technology")

    price: Mapped[float] = mapped_column(Float, default=0.0)
    change_abs: Mapped[float] = mapped_column(Float, default=0.0)
    change_pct: Mapped[float] = mapped_column(Float, default=0.0)
    market_cap_usd: Mapped[float] = mapped_column(Float, default=0.0)
    market_cap_label: Mapped[str] = mapped_column(String(64), default="$0")
    pe_ratio: Mapped[str] = mapped_column(String(32), default="—")
    dividend_annual: Mapped[str] = mapped_column(String(32), default="$0,00")
    dividend_yield: Mapped[str] = mapped_column(String(32), default="0,00%")
    dividend_per_quarter: Mapped[float] = mapped_column(Float, default=0.0)

    # AAOIFI screening raw ratios & verdict
    debt_ratio_pct: Mapped[float] = mapped_column(Float, default=0.0)
    interest_deposit_ratio_pct: Mapped[float] = mapped_column(Float, default=0.0)
    impure_revenue_pct: Mapped[float] = mapped_column(Float, default=0.0)
    business_status: Mapped[str] = mapped_column(String(16), default="HALAL")
    halal_status: Mapped[str] = mapped_column(String(16), index=True, default="HALAL")
    purification_per_dividend: Mapped[float] = mapped_column(Float, default=0.0)
    verdict_summary: Mapped[str] = mapped_column(String(255), default="")
    verdict_subtitle: Mapped[str] = mapped_column(String(255), default="Методика AAOIFI / WeGrow")
    report_period: Mapped[str] = mapped_column(String(128), default="Отчётность за III квартал 2026")
    checked_date: Mapped[str] = mapped_column(String(32), default="25.11.2026")
    manual_override: Mapped[bool] = mapped_column(Boolean, default=False)

    revenue_segments_json: Mapped[str] = mapped_column(Text, default="[]")
    criteria_json: Mapped[str] = mapped_column(Text, default="[]")
    charts_json: Mapped[str] = mapped_column(Text, default="{}")
    data_source: Mapped[str] = mapped_column(String(32), default="seed")
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=_utcnow)


class Goal(Base):
    __tablename__ = "goals"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"), index=True)
    title: Mapped[str] = mapped_column(String(255))
    title_uz: Mapped[str] = mapped_column(String(255))
    target_amount: Mapped[float] = mapped_column(Float)
    current_amount: Mapped[float] = mapped_column(Float, default=0.0)
    monthly_contribution: Mapped[float] = mapped_column(Float, default=200.0)
    deadline_label: Mapped[str] = mapped_column(String(128), default="Декабрь 2027")
    deadline_label_uz: Mapped[str] = mapped_column(String(128), default="Dekabr 2027")
    category: Mapped[str] = mapped_column(String(64), default="savings")
    icon: Mapped[str] = mapped_column(String(64), default="target")
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=_utcnow)

    user: Mapped["User"] = relationship("User", back_populates="goals")

    def to_dict(self) -> dict:
        return {
            "id": self.id,
            "title": self.title,
            "title_uz": self.title_uz,
            "target_amount": self.target_amount,
            "current_amount": self.current_amount,
            "monthly_contribution": self.monthly_contribution,
            "deadline_label": self.deadline_label,
            "deadline_label_uz": self.deadline_label_uz,
            "category": self.category,
            "icon": self.icon,
        }


class Course(Base):
    __tablename__ = "courses"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    payload_json: Mapped[str] = mapped_column(Text)


class UserCoursePurchase(Base):
    __tablename__ = "user_course_purchases"
    __table_args__ = (UniqueConstraint("user_id", "course_id", name="uq_user_course"),)

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"), index=True)
    course_id: Mapped[int] = mapped_column(ForeignKey("courses.id", ondelete="CASCADE"), index=True)
    provider: Mapped[str] = mapped_column(String(32), default="Payme")
    purchased_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=_utcnow)

    user: Mapped["User"] = relationship("User", back_populates="course_purchases")

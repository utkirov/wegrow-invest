import json
import math
import os
import sqlite3
from contextlib import contextmanager
from datetime import datetime
from typing import List, Optional

from fastapi import FastAPI, HTTPException, Query
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel

DB_PATH = os.path.join(os.path.dirname(__file__), "wegrow.db")

app = FastAPI(
    title="WeGrow Invest API v3.0",
    description="Backend API for WeGrow Invest Android App (Halal Stock Analysis, Courses, Personal Finance & Goals)",
    version="3.0.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@contextmanager
def get_db():
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    try:
        yield conn
        conn.commit()
    finally:
        conn.close()


class ProfileUpdate(BaseModel):
    first_name: str
    last_name: str
    middle_name: Optional[str] = ""
    birth_date: Optional[str] = "15.04.1996"
    gender: Optional[str] = "male"
    language: Optional[str] = "ru"
    dark_theme: Optional[bool] = True


class GoalCreate(BaseModel):
    title: str
    target_amount: float
    current_amount: float = 0.0
    monthly_contribution: float = 200.0
    deadline_label: str = "Декабрь 2027"
    category: str = "savings"
    icon: str = "target"


class GoalContribute(BaseModel):
    amount: float


class CoursePurchaseRequest(BaseModel):
    provider: str = "Payme"


INITIAL_STOCKS = [
    {
        "symbol": "AAPL",
        "name": "Apple Inc.",
        "exchange": "NASDAQ",
        "sector": "Технологии",
        "sector_uz": "Texnologiyalar",
        "price": 228.45,
        "change_abs": 2.31,
        "change_pct": 1.02,
        "halal_status": "HALAL",
        "checked_date": "08.11.2026",
        "market_cap": "$3,45 трлн",
        "pe_ratio": "34,8",
        "dividend_annual": "$1,00",
        "dividend_yield": "0,44%",
        "purification_per_dividend": 0.002,
        "dividend_per_quarter": 0.25,
        "verdict_summary": "Соответствует 4 из 4 критериев",
        "verdict_subtitle": "Методика WeGrow, пересмотр раз в квартал",
        "report_period": "Отчётность за III квартал 2026",
        "revenue_segments": [
            {"label": "Устройства", "label_uz": "Qurilmalar", "pct": 52.0, "type": "halal"},
            {"label": "Сервисы", "label_uz": "Servislar", "pct": 26.0, "type": "accent"},
            {"label": "Прочее", "label_uz": "Boshqa", "pct": 21.1, "type": "muted"},
            {"label": "Недопустимо", "label_uz": "Nomaqbul", "pct": 0.9, "type": "haram"},
        ],
        "criteria": [
            {"id": "business", "title": "Деятельность", "title_uz": "Faoliyat", "value_label": "OK", "subtitle": "допустима", "subtitle_uz": "maqbul", "pct": 100.0, "limit_pct": 100.0, "status": "HALAL"},
            {"id": "debt", "title": "Долг", "title_uz": "Qarz yuki", "value_label": "9,8%", "subtitle": "лимит 30%", "subtitle_uz": "limit 30%", "pct": 9.8, "limit_pct": 30.0, "status": "HALAL"},
            {"id": "deposits", "title": "Депозиты", "title_uz": "Foizli depozitlar", "value_label": "4,6%", "subtitle": "лимит 30%", "subtitle_uz": "limit 30%", "pct": 4.6, "limit_pct": 30.0, "status": "HALAL"},
            {"id": "impure", "title": "Недоп. доход", "title_uz": "Nomaqbul daromad", "value_label": "0,9%", "subtitle": "лимит 5%", "subtitle_uz": "limit 5%", "pct": 0.9, "limit_pct": 5.0, "status": "HALAL"},
        ],
        "chart_1m": [221.0, 222.4, 220.8, 223.5, 225.1, 224.0, 226.8, 225.9, 227.4, 229.2, 227.8, 228.45],
    },
    {
        "symbol": "NVDA",
        "name": "NVIDIA Corp.",
        "exchange": "NASDAQ",
        "sector": "Технологии",
        "sector_uz": "Texnologiyalar",
        "price": 134.20,
        "change_abs": 4.44,
        "change_pct": 3.42,
        "halal_status": "HALAL",
        "checked_date": "10.11.2026",
        "market_cap": "$3,29 трлн",
        "pe_ratio": "52,4",
        "dividend_annual": "$0,04",
        "dividend_yield": "0,03%",
        "purification_per_dividend": 0.0001,
        "dividend_per_quarter": 0.01,
        "verdict_summary": "Соответствует 4 из 4 критериев",
        "verdict_subtitle": "Методика WeGrow, пересмотр раз в квартал",
        "report_period": "Отчётность за III квартал 2026",
        "revenue_segments": [
            {"label": "Дата-центры и ИИ", "label_uz": "Data-markaz va AI", "pct": 87.0, "type": "halal"},
            {"label": "Игровые GPU", "label_uz": "Gaming GPU", "pct": 9.5, "type": "accent"},
            {"label": "Авто и прочее", "label_uz": "Avto va boshqa", "pct": 3.2, "type": "muted"},
            {"label": "Недопустимо", "label_uz": "Nomaqbul", "pct": 0.3, "type": "haram"},
        ],
        "criteria": [
            {"id": "business", "title": "Деятельность", "title_uz": "Faoliyat", "value_label": "OK", "subtitle": "допустима", "subtitle_uz": "maqbul", "pct": 100.0, "limit_pct": 100.0, "status": "HALAL"},
            {"id": "debt", "title": "Долг", "title_uz": "Qarz yuki", "value_label": "3,1%", "subtitle": "лимит 30%", "subtitle_uz": "limit 30%", "pct": 3.1, "limit_pct": 30.0, "status": "HALAL"},
            {"id": "deposits", "title": "Депозиты", "title_uz": "Foizli depozitlar", "value_label": "8,4%", "subtitle": "лимит 30%", "subtitle_uz": "limit 30%", "pct": 8.4, "limit_pct": 30.0, "status": "HALAL"},
            {"id": "impure", "title": "Недоп. доход", "title_uz": "Nomaqbul daromad", "value_label": "0,3%", "subtitle": "лимит 5%", "subtitle_uz": "limit 5%", "pct": 0.3, "limit_pct": 5.0, "status": "HALAL"},
        ],
        "chart_1m": [121.5, 123.0, 122.2, 125.4, 127.8, 126.9, 129.5, 131.0, 130.2, 132.6, 133.1, 134.20],
    },
    {
        "symbol": "MSFT",
        "name": "Microsoft Corp.",
        "exchange": "NASDAQ",
        "sector": "Технологии",
        "sector_uz": "Texnologiyalar",
        "price": 426.80,
        "change_abs": 7.62,
        "change_pct": 1.82,
        "halal_status": "HALAL",
        "checked_date": "09.11.2026",
        "market_cap": "$3,17 трлн",
        "pe_ratio": "35,6",
        "dividend_annual": "$3,32",
        "dividend_yield": "0,78%",
        "purification_per_dividend": 0.011,
        "dividend_per_quarter": 0.83,
        "verdict_summary": "Соответствует 4 из 4 критериев",
        "verdict_subtitle": "Методика WeGrow, пересмотр раз в квартал",
        "report_period": "Отчётность за III квартал 2026",
        "revenue_segments": [
            {"label": "Azure и Облако", "label_uz": "Azure va Bulut", "pct": 54.0, "type": "halal"},
            {"label": "Office и Бизнес", "label_uz": "Office va Biznes", "pct": 32.0, "type": "accent"},
            {"label": "Windows и Устройства", "label_uz": "Windows va Qurilmalar", "pct": 12.6, "type": "muted"},
            {"label": "Недопустимо", "label_uz": "Nomaqbul", "pct": 1.4, "type": "haram"},
        ],
        "criteria": [
            {"id": "business", "title": "Деятельность", "title_uz": "Faoliyat", "value_label": "OK", "subtitle": "допустима", "subtitle_uz": "maqbul", "pct": 100.0, "limit_pct": 100.0, "status": "HALAL"},
            {"id": "debt", "title": "Долг", "title_uz": "Qarz yuki", "value_label": "6,2%", "subtitle": "лимит 30%", "subtitle_uz": "limit 30%", "pct": 6.2, "limit_pct": 30.0, "status": "HALAL"},
            {"id": "deposits", "title": "Депозиты", "title_uz": "Foizli depozitlar", "value_label": "7,1%", "subtitle": "лимит 30%", "subtitle_uz": "limit 30%", "pct": 7.1, "limit_pct": 30.0, "status": "HALAL"},
            {"id": "impure", "title": "Недоп. доход", "title_uz": "Nomaqbul daromad", "value_label": "1,4%", "subtitle": "лимит 5%", "subtitle_uz": "limit 5%", "pct": 1.4, "limit_pct": 5.0, "status": "HALAL"},
        ],
        "chart_1m": [412.0, 414.5, 413.2, 417.0, 419.5, 418.1, 421.0, 422.4, 420.9, 424.0, 425.2, 426.80],
    },
    {
        "symbol": "LLY",
        "name": "Eli Lilly and Co.",
        "exchange": "NYSE",
        "sector": "Здравоохранение",
        "sector_uz": "Sog'liqni saqlash",
        "price": 842.10,
        "change_abs": 12.40,
        "change_pct": 1.49,
        "halal_status": "HALAL",
        "checked_date": "07.11.2026",
        "market_cap": "$802 млрд",
        "pe_ratio": "64,2",
        "dividend_annual": "$5,20",
        "dividend_yield": "0,62%",
        "purification_per_dividend": 0.008,
        "dividend_per_quarter": 1.30,
        "verdict_summary": "Соответствует 4 из 4 критериев",
        "verdict_subtitle": "Методика WeGrow, пересмотр раз в квартал",
        "report_period": "Отчётность за III квартал 2026",
        "revenue_segments": [
            {"label": "Эндокринология", "label_uz": "Endokrinologiya", "pct": 62.0, "type": "halal"},
            {"label": "Онкология", "label_uz": "Onkologiya", "pct": 21.0, "type": "accent"},
            {"label": "Иммунология", "label_uz": "Immunologiya", "pct": 16.4, "type": "muted"},
            {"label": "Недопустимо", "label_uz": "Nomaqbul", "pct": 0.6, "type": "haram"},
        ],
        "criteria": [
            {"id": "business", "title": "Деятельность", "title_uz": "Faoliyat", "value_label": "OK", "subtitle": "допустима", "subtitle_uz": "maqbul", "pct": 100.0, "limit_pct": 100.0, "status": "HALAL"},
            {"id": "debt", "title": "Долг", "title_uz": "Qarz yuki", "value_label": "11,4%", "subtitle": "лимит 30%", "subtitle_uz": "limit 30%", "pct": 11.4, "limit_pct": 30.0, "status": "HALAL"},
            {"id": "deposits", "title": "Депозиты", "title_uz": "Foizli depozitlar", "value_label": "2,1%", "subtitle": "лимит 30%", "subtitle_uz": "limit 30%", "pct": 2.1, "limit_pct": 30.0, "status": "HALAL"},
            {"id": "impure", "title": "Недоп. доход", "title_uz": "Nomaqbul daromad", "value_label": "0,6%", "subtitle": "лимит 5%", "subtitle_uz": "limit 5%", "pct": 0.6, "limit_pct": 5.0, "status": "HALAL"},
        ],
        "chart_1m": [810.0, 815.0, 812.5, 820.0, 825.4, 823.0, 829.0, 834.2, 831.0, 837.5, 839.0, 842.10],
    },
    {
        "symbol": "AMZN",
        "name": "Amazon.com Inc.",
        "exchange": "NASDAQ",
        "sector": "Потребление",
        "sector_uz": "Iste'mol",
        "price": 210.35,
        "change_abs": 1.85,
        "change_pct": 0.89,
        "halal_status": "DOUBTFUL",
        "checked_date": "06.11.2026",
        "market_cap": "$2,21 трлн",
        "pe_ratio": "42,1",
        "dividend_annual": "$0,00",
        "dividend_yield": "0,00%",
        "purification_per_dividend": 0.0,
        "dividend_per_quarter": 0.0,
        "verdict_summary": "Пограничная доля медиа и финтех-сервисов",
        "verdict_subtitle": "Методика WeGrow, пересмотр раз в квартал",
        "report_period": "Отчётность за III квартал 2026",
        "revenue_segments": [
            {"label": "AWS и Облако", "label_uz": "AWS va Bulut", "pct": 48.0, "type": "halal"},
            {"label": "Ритейл и Логистика", "label_uz": "Riteyl va Logistika", "pct": 41.0, "type": "accent"},
            {"label": "Подписки и Медиа", "label_uz": "Obuna va Media", "pct": 6.8, "type": "muted"},
            {"label": "Недопустимо", "label_uz": "Nomaqbul", "pct": 4.2, "type": "haram"},
        ],
        "criteria": [
            {"id": "business", "title": "Деятельность", "title_uz": "Faoliyat", "value_label": "Спорно", "subtitle": "Prime Video / кредиты", "subtitle_uz": "Prime Video / kreditlar", "pct": 75.0, "limit_pct": 100.0, "status": "DOUBTFUL"},
            {"id": "debt", "title": "Долг", "title_uz": "Qarz yuki", "value_label": "14,2%", "subtitle": "лимит 30%", "subtitle_uz": "limit 30%", "pct": 14.2, "limit_pct": 30.0, "status": "HALAL"},
            {"id": "deposits", "title": "Депозиты", "title_uz": "Foizli depozitlar", "value_label": "8,9%", "subtitle": "лимит 30%", "subtitle_uz": "limit 30%", "pct": 8.9, "limit_pct": 30.0, "status": "HALAL"},
            {"id": "impure", "title": "Недоп. доход", "title_uz": "Nomaqbul daromad", "value_label": "4,2%", "subtitle": "лимит 5%", "subtitle_uz": "limit 5%", "pct": 4.2, "limit_pct": 5.0, "status": "DOUBTFUL"},
        ],
        "chart_1m": [198.0, 200.5, 199.2, 202.0, 204.5, 203.1, 206.0, 207.4, 205.9, 208.0, 209.2, 210.35],
    },
    {
        "symbol": "JPM",
        "name": "JPMorgan Chase & Co.",
        "exchange": "NYSE",
        "sector": "Финансы",
        "sector_uz": "Moliya",
        "price": 238.90,
        "change_abs": -1.45,
        "change_pct": -0.60,
        "halal_status": "HARAM",
        "checked_date": "05.11.2026",
        "market_cap": "$675 млрд",
        "pe_ratio": "13,2",
        "dividend_annual": "$5,00",
        "dividend_yield": "2,09%",
        "purification_per_dividend": 0.0,
        "dividend_per_quarter": 1.25,
        "verdict_summary": "Не соответствует 3 из 4 критериев (процентный банк)",
        "verdict_subtitle": "Методика WeGrow, пересмотр раз в квартал",
        "report_period": "Отчётность за III квартал 2026",
        "revenue_segments": [
            {"label": "Комиссии", "label_uz": "Komissiyalar", "pct": 12.0, "type": "halal"},
            {"label": "Процентный доход", "label_uz": "Foizli daromad", "pct": 88.0, "type": "haram"},
        ],
        "criteria": [
            {"id": "business", "title": "Деятельность", "title_uz": "Faoliyat", "value_label": "Харам", "subtitle": "традиционный банк", "subtitle_uz": "an'anaviy bank", "pct": 100.0, "limit_pct": 100.0, "status": "HARAM"},
            {"id": "debt", "title": "Долг", "title_uz": "Qarz yuki", "value_label": "68,0%", "subtitle": "лимит 30%", "subtitle_uz": "limit 30%", "pct": 68.0, "limit_pct": 30.0, "status": "HARAM"},
            {"id": "deposits", "title": "Депозиты", "title_uz": "Foizli depozitlar", "value_label": "74,5%", "subtitle": "лимит 30%", "subtitle_uz": "limit 30%", "pct": 74.5, "limit_pct": 30.0, "status": "HARAM"},
            {"id": "impure", "title": "Недоп. доход", "title_uz": "Nomaqbul daromad", "value_label": "88,0%", "subtitle": "лимит 5%", "subtitle_uz": "limit 5%", "pct": 88.0, "limit_pct": 5.0, "status": "HARAM"},
        ],
        "chart_1m": [242.0, 243.5, 241.2, 240.0, 241.5, 239.8, 240.2, 239.4, 240.1, 239.5, 240.35, 238.90],
    },
]

INITIAL_GOALS = [
    {
        "id": 1,
        "title": "Финансовая подушка",
        "title_uz": "Moliyaviy yostiqcha",
        "target_amount": 3000.0,
        "current_amount": 2100.0,
        "monthly_contribution": 250.0,
        "deadline_label": "Март 2027 · осталось 4 мес",
        "deadline_label_uz": "Mart 2027 · 4 oy qoldi",
        "category": "safety",
        "icon": "shield-check",
    },
    {
        "id": 2,
        "title": "Умра для родителей",
        "title_uz": "Ota-ona uchun Umra",
        "target_amount": 6000.0,
        "current_amount": 1950.0,
        "monthly_contribution": 300.0,
        "deadline_label": "Рамадан 2027 · по плану",
        "deadline_label_uz": "Ramazon 2027 · reja bo'yicha",
        "category": "family",
        "icon": "plane",
    },
    {
        "id": 3,
        "title": "Новый ноутбук",
        "title_uz": "Yangi noutbuk",
        "target_amount": 3000.0,
        "current_amount": 800.0,
        "monthly_contribution": 150.0,
        "deadline_label": "Август 2027 · +$100 в ноябре",
        "deadline_label_uz": "Avgust 2027 · noyabrda +$100",
        "category": "tech",
        "icon": "laptop",
    },
]

INITIAL_COURSES = [
    {
        "id": 1,
        "title": "Основы инвестирования в акции США",
        "title_uz": "AQSh aksiyalariga investitsiya asoslari",
        "level": "Начальный уровень",
        "level_uz": "Boshlang'ich daraja",
        "rating": 4.9,
        "reviews_count": 1240,
        "author": "Команда аналитиков WeGrow",
        "author_uz": "WeGrow tahlilchilar jamoasi",
        "author_role": "Авторы курса",
        "author_role_uz": "Kurs mualliflari",
        "preview_duration": "2 мин 14 сек",
        "modules_count": 8,
        "lessons_count": 42,
        "access_duration": "6 месяцев",
        "access_duration_uz": "6 oy",
        "price_uzs": "1 290 000 сум",
        "price_uzs_num": 1290000,
        "purchased": False,
        "modules": [
            {"number": 1, "title": "Как устроен фондовый рынок США", "title_uz": "AQSh fond bozori qanday ishlaydi", "meta": "5 уроков · 45 мин · Бесплатно", "meta_uz": "5 dars · 45 daq · Bepul", "locked": False},
            {"number": 2, "title": "Критерии халяльности акций (AAOIFI и WeGrow)", "title_uz": "Aksiyalar halollik mezonlari (AAOIFI va WeGrow)", "meta": "6 уроков · 1 ч 10 мин", "meta_uz": "6 dars · 1 s 10 daq", "locked": True},
            {"number": 3, "title": "Финансовая отчётность и мультипликаторы P/E, ROE", "title_uz": "Moliyaviy hisobot va P/E, ROE ko'rsatkichlari", "meta": "6 уроков · 1 ч 20 мин", "meta_uz": "6 dars · 1 s 20 daq", "locked": True},
            {"number": 4, "title": "Очистка дивидендов (тазкия) и закят с инвестиций", "title_uz": "Dividendlarni poklash va investitsiya zakoti", "meta": "5 уроков · 50 мин", "meta_uz": "5 dars · 50 daq", "locked": True},
            {"number": 5, "title": "Выбор брокера в Узбекистане и пополнение счёта", "title_uz": "O'zbekistonda broker tanlash va hisobni to'ldirish", "meta": "4 урока · 40 мин", "meta_uz": "4 dars · 40 daq", "locked": True},
            {"number": 6, "title": "Сборка долгосрочного халяль-портфеля", "title_uz": "Uzoq muddatli halol portfel yig'ish", "meta": "6 уроков · 1 ч 15 мин", "meta_uz": "6 dars · 1 s 15 daq", "locked": True},
            {"number": 7, "title": "Управление рисками и психология инвестора", "title_uz": "Xatarlarni boshqarish va investor psixologiyasi", "meta": "5 уроков · 55 мин", "meta_uz": "5 dars · 55 daq", "locked": True},
            {"number": 8, "title": "Итоговый проект и сертификация WeGrow", "title_uz": "Yakuniy loyiha va WeGrow sertifikati", "meta": "5 уроков · Экзамен", "meta_uz": "5 dars · Imtihon", "locked": True},
        ],
    }
]

INITIAL_NEWS = [
    {
        "id": 1,
        "title": "NVIDIA обновила рекорд выручки в сегменте ИИ-чипов за III квартал",
        "title_uz": "NVIDIA III chorakda AI-chiplar segmentida rekord daromad ko'rsatdi",
        "source": "Рынок США · 25 мин назад",
        "source_uz": "AQSh bozori · 25 daqiqa oldin",
        "ticker": "NVDA",
        "tag": "Халяль",
    },
    {
        "id": 2,
        "title": "Итоги квартального пересмотра халяль-статусов индекса S&P 500 по методике WeGrow",
        "title_uz": "WeGrow metodikasi bo'yicha S&P 500 halol maqomlari choraklik sharhi",
        "source": "Аналитика WeGrow · 2 ч назад",
        "source_uz": "WeGrow tahlili · 2 soat oldin",
        "ticker": "AAPL",
        "tag": "Методика",
    },
    {
        "id": 3,
        "title": "Apple расширяет линейку чипов M4 и сохраняет долговую нагрузку ниже 10%",
        "title_uz": "Apple M4 chiplarini kengaytirmoqda va qarz yukini 10% dan past saqlamoqda",
        "source": "Технологии · 4 ч назад",
        "source_uz": "Texnologiyalar · 4 soat oldin",
        "ticker": "AAPL",
        "tag": "Отчётность",
    },
]


def init_db():
    with get_db() as conn:
        conn.execute(
            """
            CREATE TABLE IF NOT EXISTS profile (
                id INTEGER PRIMARY KEY CHECK (id = 1),
                first_name TEXT NOT NULL,
                last_name TEXT NOT NULL,
                middle_name TEXT NOT NULL,
                birth_date TEXT NOT NULL,
                gender TEXT NOT NULL,
                language TEXT NOT NULL,
                dark_theme INTEGER NOT NULL
            )
            """
        )
        conn.execute(
            """
            CREATE TABLE IF NOT EXISTS stocks (
                symbol TEXT PRIMARY KEY,
                payload TEXT NOT NULL
            )
            """
        )
        conn.execute(
            """
            CREATE TABLE IF NOT EXISTS goals (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                title_uz TEXT NOT NULL,
                target_amount REAL NOT NULL,
                current_amount REAL NOT NULL,
                monthly_contribution REAL NOT NULL,
                deadline_label TEXT NOT NULL,
                deadline_label_uz TEXT NOT NULL,
                category TEXT NOT NULL,
                icon TEXT NOT NULL
            )
            """
        )
        conn.execute(
            """
            CREATE TABLE IF NOT EXISTS courses (
                id INTEGER PRIMARY KEY,
                payload TEXT NOT NULL
            )
            """
        )

        cur = conn.execute("SELECT COUNT(*) FROM profile")
        if cur.fetchone()[0] == 0:
            conn.execute(
                """
                INSERT INTO profile (id, first_name, last_name, middle_name, birth_date, gender, language, dark_theme)
                VALUES (1, 'Азиз', 'Каримов', 'Рустамович', '15.04.1996', 'male', 'ru', 1)
                """
            )

        cur = conn.execute("SELECT COUNT(*) FROM stocks")
        if cur.fetchone()[0] == 0:
            for st in INITIAL_STOCKS:
                conn.execute(
                    "INSERT INTO stocks (symbol, payload) VALUES (?, ?)",
                    (st["symbol"], json.dumps(st, ensure_ascii=False)),
                )

        cur = conn.execute("SELECT COUNT(*) FROM goals")
        if cur.fetchone()[0] == 0:
            for g in INITIAL_GOALS:
                conn.execute(
                    """
                    INSERT INTO goals (title, title_uz, target_amount, current_amount, monthly_contribution, deadline_label, deadline_label_uz, category, icon)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    (
                        g["title"],
                        g["title_uz"],
                        g["target_amount"],
                        g["current_amount"],
                        g["monthly_contribution"],
                        g["deadline_label"],
                        g["deadline_label_uz"],
                        g["category"],
                        g["icon"],
                    ),
                )

        cur = conn.execute("SELECT COUNT(*) FROM courses")
        if cur.fetchone()[0] == 0:
            for c in INITIAL_COURSES:
                conn.execute(
                    "INSERT INTO courses (id, payload) VALUES (?, ?)",
                    (c["id"], json.dumps(c, ensure_ascii=False)),
                )


init_db()


@app.get("/api/health")
def health():
    return {
        "status": "ok",
        "service": "WeGrow Invest API v3.0",
        "timestamp": datetime.utcnow().isoformat() + "Z",
    }


@app.get("/api/bootstrap")
def bootstrap():
    with get_db() as conn:
        p_row = conn.execute("SELECT * FROM profile WHERE id = 1").fetchone()
        profile = dict(p_row)
        profile["dark_theme"] = bool(profile["dark_theme"])

        s_rows = conn.execute("SELECT payload FROM stocks").fetchall()
        stocks = [json.loads(r["payload"]) for r in s_rows]

        g_rows = conn.execute("SELECT * FROM goals ORDER BY id ASC").fetchall()
        goals = [dict(r) for r in g_rows]

        c_rows = conn.execute("SELECT payload FROM courses").fetchall()
        courses = [json.loads(r["payload"]) for r in c_rows]

    total_saved = sum(g["current_amount"] for g in goals)
    total_target = sum(g["target_amount"] for g in goals)

    return {
        "profile": profile,
        "goals_summary": {
            "total_saved": total_saved,
            "total_target": total_target,
            "progress_pct": round((total_saved / total_target * 100.0) if total_target > 0 else 0.0, 1),
            "monthly_delta_pct": 12.8,
            "monthly_delta_usd": 350.0,
            "ahead_label": "Опережаете план на 2 недели",
            "ahead_label_uz": "Rejadan 2 hafta oldindasiz",
        },
        "market_overview": {
            "is_open": True,
            "status_text": "Биржи США открыты, до закрытия 2 ч 14 мин",
            "status_text_uz": "AQSh birjalari ochiq, yopilishiga 2 s 14 daq",
            "indices": [
                {"name": "S&P 500", "value": "5 912,4", "change_pct": "+0,84%", "is_up": True},
                {"name": "NASDAQ", "value": "19 120,8", "change_pct": "+1,42%", "is_up": True},
                {"name": "Dow Jones", "value": "43 870,4", "change_pct": "-0,18%", "is_up": False},
            ],
            "heatmap": [
                {"name": "Технологии", "name_uz": "Texnologiyalar", "change_pct": "+2,14%", "tone": "UP_3", "weight": 2.2},
                {"name": "Коммуникации", "name_uz": "Kommunikatsiya", "change_pct": "+1,12%", "tone": "UP_2", "weight": 1.6},
                {"name": "Здравоохр.", "name_uz": "Sog'liqni saqlash", "change_pct": "+0,95%", "tone": "UP_2", "weight": 1.4},
                {"name": "Пром.", "name_uz": "Sanoat", "change_pct": "+0,34%", "tone": "UP_1", "weight": 1.1},
                {"name": "Потребление", "name_uz": "Iste'mol", "change_pct": "+0,41%", "tone": "UP_1", "weight": 1.3},
                {"name": "Сырьё", "name_uz": "Xomashyo", "change_pct": "-0,28%", "tone": "DOWN_1", "weight": 0.9},
                {"name": "Энергетика", "name_uz": "Energetika", "change_pct": "-0,86%", "tone": "DOWN_2", "weight": 1.0},
                {"name": "Недвиж.", "name_uz": "Ko'chmas mulk", "change_pct": "+0,19%", "tone": "UP_1", "weight": 1.0},
            ],
        },
        "stocks": stocks,
        "goals": goals,
        "courses": courses,
        "news": INITIAL_NEWS,
    }


@app.get("/api/stocks")
def list_stocks(
    q: Optional[str] = Query(default=None),
    halal_only: bool = Query(default=False),
    sector: Optional[str] = Query(default=None),
):
    with get_db() as conn:
        rows = conn.execute("SELECT payload FROM stocks").fetchall()
        stocks = [json.loads(r["payload"]) for r in rows]

    if halal_only:
        stocks = [s for s in stocks if s["halal_status"] == "HALAL"]
    if sector and sector not in ("Все", "Barchasi"):
        stocks = [s for s in stocks if s["sector"].lower() == sector.lower() or s.get("sector_uz", "").lower() == sector.lower()]
    if q:
        ql = q.strip().lower()
        stocks = [
            s for s in stocks
            if ql in s["symbol"].lower() or ql in s["name"].lower() or ql in s["sector"].lower()
        ]
    return {"stocks": stocks}


@app.get("/api/stocks/{symbol}")
def get_stock(symbol: str):
    with get_db() as conn:
        row = conn.execute("SELECT payload FROM stocks WHERE UPPER(symbol) = UPPER(?)", (symbol,)).fetchone()
        if not row:
            raise HTTPException(status_code=404, detail="Stock not found")
        return json.loads(row["payload"])


@app.post("/api/profile")
def update_profile(body: ProfileUpdate):
    with get_db() as conn:
        conn.execute(
            """
            UPDATE profile
            SET first_name = ?, last_name = ?, middle_name = ?, birth_date = ?, gender = ?, language = ?, dark_theme = ?
            WHERE id = 1
            """,
            (
                body.first_name,
                body.last_name,
                body.middle_name or "",
                body.birth_date or "15.04.1996",
                body.gender or "male",
                body.language or "ru",
                1 if body.dark_theme else 0,
            ),
        )
        p_row = conn.execute("SELECT * FROM profile WHERE id = 1").fetchone()
        profile = dict(p_row)
        profile["dark_theme"] = bool(profile["dark_theme"])
        return {"status": "ok", "profile": profile}


@app.get("/api/goals")
def list_goals():
    with get_db() as conn:
        rows = conn.execute("SELECT * FROM goals ORDER BY id ASC").fetchall()
        goals = [dict(r) for r in rows]
    return {"goals": goals}


@app.post("/api/goals")
def create_goal(body: GoalCreate):
    with get_db() as conn:
        cur = conn.execute(
            """
            INSERT INTO goals (title, title_uz, target_amount, current_amount, monthly_contribution, deadline_label, deadline_label_uz, category, icon)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            (
                body.title,
                body.title,
                body.target_amount,
                body.current_amount,
                body.monthly_contribution,
                body.deadline_label,
                body.deadline_label,
                body.category,
                body.icon,
            ),
        )
        new_id = cur.lastrowid
        row = conn.execute("SELECT * FROM goals WHERE id = ?", (new_id,)).fetchone()
        return {"status": "ok", "goal": dict(row)}


@app.post("/api/goals/{goal_id}/contribute")
def contribute_goal(goal_id: int, body: GoalContribute):
    with get_db() as conn:
        row = conn.execute("SELECT * FROM goals WHERE id = ?", (goal_id,)).fetchone()
        if not row:
            raise HTTPException(status_code=404, detail="Goal not found")
        new_amount = min(row["target_amount"], max(0.0, row["current_amount"] + body.amount))
        conn.execute("UPDATE goals SET current_amount = ? WHERE id = ?", (new_amount, goal_id))
        updated = conn.execute("SELECT * FROM goals WHERE id = ?", (goal_id,)).fetchone()
        return {"status": "ok", "goal": dict(updated)}


@app.delete("/api/goals/{goal_id}")
def delete_goal(goal_id: int):
    with get_db() as conn:
        conn.execute("DELETE FROM goals WHERE id = ?", (goal_id,))
    return {"status": "ok"}


@app.post("/api/courses/{course_id}/purchase")
def purchase_course(course_id: int, body: CoursePurchaseRequest):
    with get_db() as conn:
        row = conn.execute("SELECT payload FROM courses WHERE id = ?", (course_id,)).fetchone()
        if not row:
            raise HTTPException(status_code=404, detail="Course not found")
        course = json.loads(row["payload"])
        course["purchased"] = True
        course["purchased_via"] = body.provider
        for m in course.get("modules", []):
            m["locked"] = False
        conn.execute(
            "UPDATE courses SET payload = ? WHERE id = ?",
            (json.dumps(course, ensure_ascii=False), course_id),
        )
        return {"status": "ok", "course": course}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8090)

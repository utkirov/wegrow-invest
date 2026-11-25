import json

from sqlalchemy.orm import Session

from app.db.models import CourseLesson

INITIAL_LESSONS = [
    {
        "course_id": 1,
        "module_number": 1,
        "lesson_number": 1,
        "title": "Как устроен фондовый рынок США (NYSE и NASDAQ)",
        "title_uz": "AQSh fond bozori qanday ishlaydi (NYSE va NASDAQ)",
        "lesson_type": "video",
        "duration_min": 12,
        "video_url": "https://cdn.wegrow.uz/courses/1/m1_l1.m3u8",
        "content_md": (
            "## Основы фондового рынка США\n\n"
            "Фондовый рынок США — крупнейший в мире рынок капитала. Основные площадки — **NYSE** и **NASDAQ**.\n\n"
            "- Акция даёт долю владения в реальном бизнесе.\n"
            "- Доход формируется из роста стоимости компании и дивидендов.\n"
            "- Осознанный инвестор покупает бизнес на горизонте 3–10+ лет, а не спекулирует."
        ),
        "content_md_uz": (
            "## AQSh fond bozori asoslari\n\n"
            "AQSh fond bozori dunyodagi eng yirik kapital bozoridir. Asosiy birjalar — **NYSE** va **NASDAQ**.\n\n"
            "- Aksiya real biznesdagi ulushni beradi.\n"
            "- Daromad kompaniya qiymatining o'sishi va dividendlardan shakllanadi."
        ),
        "quiz": [
            {
                "id": "q1",
                "question": "Что даёт инвестору покупка обыкновенной акции?",
                "question_uz": "Oddiy aksiya sotib olish investorga nima beradi?",
                "options": [
                    "Долю владения в компании",
                    "Фиксированный банковский процент",
                    "Кредитное обязательство",
                ],
                "correct_index": 0,
            }
        ],
        "is_preview": True,
    },
    {
        "course_id": 1,
        "module_number": 1,
        "lesson_number": 2,
        "title": "Индексы S&P 500 и NASDAQ-100 простыми словами",
        "title_uz": "S&P 500 va NASDAQ-100 indekslari sodda tilda",
        "lesson_type": "article",
        "duration_min": 8,
        "video_url": None,
        "content_md": (
            "## Зачем нужны индексы\n\n"
            "**S&P 500** объединяет 500 крупнейших публичных компаний США. "
            "Для мусульманского инвестора важно отфильтровывать нехаляльные сектора (банки, алкоголь, азартные игры) — "
            "именно это автоматически делает скринер WeGrow по стандарту AAOIFI."
        ),
        "content_md_uz": (
            "## Indekslar nima uchun kerak\n\n"
            "**S&P 500** AQShning eng yirik 500 ta kompaniyasini birlashtiradi. "
            "WeGrow skrinneri AAOIFI mezonlari bo'yicha nomaqbul sektorlarni avtomatik ajratadi."
        ),
        "quiz": [],
        "is_preview": True,
    },
    {
        "course_id": 1,
        "module_number": 2,
        "lesson_number": 1,
        "title": "4 критерия халяльности акций по стандарту AAOIFI",
        "title_uz": "AAOIFI standarti bo'yicha 4 ta halollik mezoni",
        "lesson_type": "quiz",
        "duration_min": 15,
        "video_url": "https://cdn.wegrow.uz/courses/1/m2_l1.m3u8",
        "content_md": (
            "## Стандарт AAOIFI №21\n\n"
            "1. **Допустимая деятельность**: основной бизнес не связан с процентами (риба), алкоголем, азартными играми и др.\n"
            "2. **Процентный долг** `< 30%` от рыночной капитализации.\n"
            "3. **Процентные депозиты и облигации** `< 30%` от рыночной капитализации.\n"
            "4. **Недопустимый доход** `< 5%` от общей выручки компании."
        ),
        "content_md_uz": (
            "## AAOIFI №21 standarti\n\n"
            "1. **Maqbul faoliyat**: asosiy biznes ribo, alkogol va qimor bilan bog'liq emas.\n"
            "2. **Foizli qarz** bozor kapitallashuvining `< 30%` qismi.\n"
            "3. **Foizli depozitlar** `< 30%`.\n"
            "4. **Nomaqbul daromad** umumiy tushumning `< 5%` qismi."
        ),
        "quiz": [
            {
                "id": "q1",
                "question": "Каков максимальный порог процентного долга к капитализации по AAOIFI?",
                "question_uz": "AAOIFI bo'yicha foizli qarzning maksimal chegarasi qancha?",
                "options": ["Менее 50%", "Менее 30%", "Менее 5%"],
                "correct_index": 1,
            },
            {
                "id": "q2",
                "question": "Каков лимит недопустимого дохода в общей выручке компании?",
                "question_uz": "Kompaniya tushumida nomaqbul daromad limiti qancha?",
                "options": ["Менее 5%", "Менее 15%", "Менее 30%"],
                "correct_index": 0,
            },
        ],
        "is_preview": False,
    },
    {
        "course_id": 1,
        "module_number": 3,
        "lesson_number": 1,
        "title": "Как читать отчётность: выручка, маржа и P/E",
        "title_uz": "Moliyaviy hisobotni o'qish: tushum, marja va P/E",
        "lesson_type": "video",
        "duration_min": 18,
        "video_url": "https://cdn.wegrow.uz/courses/1/m3_l1.m3u8",
        "content_md": (
            "## Ключевые мультипликаторы\n\n"
            "- **P/E (Price / Earnings)** показывает, за сколько лет окупятся вложения при текущей прибыли.\n"
            "- Сравнивайте P/E только внутри одного сектора (например, полупроводники с полупроводниками)."
        ),
        "content_md_uz": (
            "## Asosiy ko'rsatkichlar\n\n"
            "- **P/E** investitsiya joriy foyda bilan necha yilda qoplanishini ko'rsatadi."
        ),
        "quiz": [],
        "is_preview": False,
    },
    {
        "course_id": 1,
        "module_number": 4,
        "lesson_number": 1,
        "title": "Очищение дивидендов (Тазкия): формула и практика",
        "title_uz": "Dividendlarni poklash (Tazkiya): formula va amaliyot",
        "lesson_type": "article",
        "duration_min": 14,
        "video_url": None,
        "content_md": (
            "## Как рассчитать сумму очищения\n\n"
            "Если компания халяльна, но имеет `0,9%` недопустимого дохода (например, проценты на остаток на счетах), "
            "инвестор перечисляет `0,9%` от полученных дивидендов на благотворительность без ожидания награды за садака.\n\n"
            "В WeGrow v4.0 раздел **Портфель → Очищение дивидендов** считает эту сумму автоматически."
        ),
        "content_md_uz": (
            "## Poklash summasini hisoblash\n\n"
            "Halol kompaniyada `0,9%` nomaqbul daromad bo'lsa, olingan dividendlarning `0,9%` qismi xayriyaga yo'naltiriladi."
        ),
        "quiz": [],
        "is_preview": False,
    },
    {
        "course_id": 1,
        "module_number": 6,
        "lesson_number": 1,
        "title": "Сборка диверсифицированного халяль-портфеля",
        "title_uz": "Diversifikatsiyalangan halol portfel yig'ish",
        "lesson_type": "article",
        "duration_min": 16,
        "video_url": None,
        "content_md": (
            "## Правило диверсификации\n\n"
            "Распределяйте капитал минимум между 4–5 халяльными секторами (Технологии, Здравоохранение, Потребление, "
            "Промышленность, Сырьё) и не держите более 15–20% портфеля в одном тикере."
        ),
        "content_md_uz": (
            "## Diversifikatsiya qoidasi\n\n"
            "Kapitalni kamida 4–5 ta halol sektor o'rtasida taqsimlang va bitta aksiyada 15–20% dan ortiq ushlamang."
        ),
        "quiz": [],
        "is_preview": False,
    },
]


def seed_lessons(db: Session) -> None:
    if db.query(CourseLesson).count() > 0:
        return
    for item in INITIAL_LESSONS:
        db.add(
            CourseLesson(
                course_id=item["course_id"],
                module_number=item["module_number"],
                lesson_number=item["lesson_number"],
                title=item["title"],
                title_uz=item["title_uz"],
                lesson_type=item["lesson_type"],
                duration_min=item["duration_min"],
                video_url=item["video_url"],
                content_md=item["content_md"],
                content_md_uz=item["content_md_uz"],
                quiz_json=json.dumps(item["quiz"], ensure_ascii=False),
                is_preview=item["is_preview"],
            )
        )

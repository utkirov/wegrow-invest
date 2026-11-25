# WeGrow Invest v4.0

Платформа для осознанных халяль-инвестиций в акции США, учёта портфеля и очищения дивидендов, обучения и управления финансовыми целями (**Android Kotlin/Jetpack Compose + Production FastAPI Backend**).

## Что нового в v4.0

- **Модульный Backend (`server/app/`)** на FastAPI + SQLAlchemy 2.0 + Alembic (SQLite для разработки / PostgreSQL 16 + Redis + Caddy HTTPS в продакшне).
- **Авторизация OTP + JWT (`/api/v1/auth/*`)**: вход по номеру телефона (`+998...`) или Email, гостевой режим, ротация refresh-токенов и полная изоляция данных пользователей.
- **AAOIFI Халяль-скринер и 105+ акций США (`/api/v1/stocks`, `/api/v1/market/overview`)**:
  - 4 критерия AAOIFI (отрасль, долг `< 30%`, процентные депозиты `< 30%`, недопустимый доход `< 5%`) и авторасчёт очищения дивидендов.
  - Поиск, фильтрация, пагинация, живой статус бирж NYSE/NASDAQ, динамическая тепловая карта секторов и графики `1D / 1W / 1M / 6M / 1Y` (`yfinance` + кэш/fallback).
- **Инвестиционный портфель, Watchlist, Алерты и Purification Ledger (`/api/v1/portfolio`, `/watchlist`, `/alerts`, `/purification`)**: средняя цена, P&L, `%` халяльности портфеля, предупреждения о нехаляльных позициях, ценовые алерты и журнал очищения дивидендов.
- **Полноценная LMS и реальные платежи Payme & Click (`/api/v1/courses/*`, `/api/v1/orders`, `/api/v1/payments/payme/rpc`, `/api/v1/payments/click/*`)**: серверный paywall уроков, автопроверка квизов, прогресс прохождения, Payme Merchant JSON-RPC 2.0 и Click SHOP API (Prepare/Complete с MD5-подписью).
- **Обновлённый Android-клиент (`android/`)**: `WeGrowViewModel` + `StateFlow`, **OkHttp + `kotlinx.serialization`** с автообновлением JWT, выделенные экраны `AuthScreen`, `PortfolioScreen`, `CourseLmsScreen`, Gradle Wrapper и R8-оптимизация.
- **Безопасность и CI/CD**: sliding-window rate limiting, security headers, Docker Compose, GitHub Actions (`.github/workflows/ci.yml`).

## Быстрый старт (разработка)

### Backend
```bash
python3 -m pip install -r server/requirements.txt
python3 server/main.py
# Документация Swagger: http://127.0.0.1:8090/docs
```

### Тесты Backend
```bash
python3 -m pytest server/tests -v
```

### Android (unit-тесты и сборка APK)
```bash
cd android
./gradlew testDebugUnitTest assembleDebug --no-daemon
# Сборка R8-оптимизированного Release APK:
./gradlew assembleRelease --no-daemon
```

## Production-деплой (Docker Compose + HTTPS)

```bash
cp server/.env.example server/.env
# Заполните боевые ключи JWT, Payme, Click и POSTGRES_PASSWORD
docker compose up -d --build
```

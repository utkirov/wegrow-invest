# WeGrow Invest v3.1 MVP

Платформа для осознанных инвестиций, халяль-скрининга акций США, обучения и управления финансовыми целями (Android + FastAPI backend).

## Структура проекта

- **`android/`** — нативное Android-приложение на **Kotlin + Jetpack Compose (Material 3)**:
  - Семантические токены тем **Dark (Лес)** и **Light** из UI Kit (`WeGrow_v3_Android_2.figcraft`).
  - Двуязычный интерфейс (**Русский / O'zbekcha**) с мгновенным переключением.
  - Оптимизировано для 120fps: `@Immutable`-модели, `drawWithCache` для графиков и узбекского узора *Khatam*, `LazyColumn`, spring-анимации и R8-минификация.
  - Экраны: Главная, Халяль-сканер, Карточка тикера с интерактивным графиком, Халяль-анализ (4 критерия + очистка дивидендов), Рынок (тепловая карта секторов + фильтр халяльных акций), Курс (превью + покупка через Payme/Click), Цели, Калькулятор сложного процента, Профиль и настройки туннеля.
- **`server/`** — REST API на **FastAPI + SQLite** (`main.py`, `requirements.txt`).
- **`start_server_and_tunnel.ps1`** — скрипт запуска локального API (`:8090`) и **Cloudflare Tunnel** (`cloudflared`).
- **`WeGrow_v3_Android.apk`** — готовый R8-оптимизированный Release APK (`~1.06 МБ`, подписан debug-ключом для быстрой установки).

## Быстрый старт сервера и туннеля

```powershell
# Установка зависимостей
pip install -r server/requirements.txt

# Запуск FastAPI + Cloudflare Tunnel
.\start_server_and_tunnel.ps1
```

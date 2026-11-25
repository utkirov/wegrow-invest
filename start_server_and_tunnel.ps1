# WeGrow Invest v3.0 — Запуск FastAPI сервера (SQLite) и Cloudflare Tunnel
Write-Host "Starting WeGrow Invest API on http://0.0.0.0:8090 ..." -ForegroundColor Green
Start-Process python -ArgumentList "`"$PSScriptRoot\server\main.py`"" -NoNewWindow

Start-Sleep -Seconds 2
Write-Host "Starting Cloudflare Tunnel (trycloudflare.com) -> http://127.0.0.1:8090 ..." -ForegroundColor Cyan
& "C:\Program Files (x86)\cloudflared\cloudflared.exe" tunnel --url http://127.0.0.1:8090 --no-autoupdate

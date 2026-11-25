FROM python:3.12-slim

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1

WORKDIR /app

RUN groupadd --system wegrow && useradd --system --gid wegrow wegrow

COPY requirements.txt requirements-prod.txt ./
RUN pip install --no-input -r requirements-prod.txt

COPY . .
RUN chown -R wegrow:wegrow /app

USER wegrow
EXPOSE 8090

HEALTHCHECK --interval=30s --timeout=5s --start-period=15s --retries=3 \
    CMD python -c "import urllib.request; urllib.request.urlopen('http://127.0.0.1:8090/api/v1/health')" || exit 1

CMD ["gunicorn", "main:app", "-k", "uvicorn.workers.UvicornWorker", "--bind", "0.0.0.0:8090", "--workers", "2", "--timeout", "60"]

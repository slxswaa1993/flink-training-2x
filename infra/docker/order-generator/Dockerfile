FROM python:3.12-slim

WORKDIR /app

# Install dependencies first (layer caching)
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy application source
COPY config.py models.py patterns.py main.py ./

# Non-root user
RUN useradd -m -u 1000 appuser
USER appuser

EXPOSE 8000

CMD ["python", "main.py"]

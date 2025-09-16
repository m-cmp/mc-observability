FROM python:3.12-slim

RUN apt-get update && apt-get install -y \
    build-essential \
    curl \
    ca-certificates \
    && rm -rf /var/lib/apt/lists/*

RUN pip install --no-cache-dir uv

WORKDIR /app

COPY . /app

RUN uv sync

EXPOSE 8002

CMD ["uv", "run", "src/server.py", "--host", "0.0.0.0", "--port", "8002", "--transport", "sse"]
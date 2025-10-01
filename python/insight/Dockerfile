FROM ghcr.io/astral-sh/uv:python3.12-bookworm-slim AS builder
ENV UV_COMPILE_BYTECODE=1 UV_LINK_MODE=copy

RUN apt-get update && apt-get install -y vim curl && apt-get clean && rm -rf /var/lib/apt/lists/*

WORKDIR /mc-insight
COPY . /mc-insight
RUN uv sync --locked

FROM python:3.12-slim-bookworm
COPY --from=builder /mc-insight /mc-insight
ENV PATH="/mc-insight/.venv/bin:$PATH"
WORKDIR /mc-insight
COPY --chmod=755 entrypoint.sh /entrypoint.sh

RUN apt-get update && apt-get install -y vim curl && apt-get clean && rm -rf /var/lib/apt/lists/*

EXPOSE 9001
ENTRYPOINT ["/entrypoint.sh"]

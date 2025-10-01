FROM ghcr.io/astral-sh/uv:python3.11-bookworm-slim AS builder
ENV UV_COMPILE_BYTECODE=1 UV_LINK_MODE=copy
USER root

# MySQL Requirements
RUN apt-get update && \
    apt-get install -y build-essential  default-libmysqlclient-dev pkg-config python3-dev

WORKDIR /airflow
COPY . /airflow
RUN uv sync --locked

FROM python:3.11-slim-bookworm
COPY --from=builder /airflow /airflow
ENV PATH="/airflow/.venv/bin:$PATH"

RUN apt-get update && \
    apt-get install -y libmariadb-dev curl

RUN rm -rf /etc/localtime && \
    ln -s /usr/share/zoneinfo/Asia/Seoul /etc/localtime

# Airflow Env Vars
ENV AIRFLOW_HOME='/usr/local/airflow'

# Set wd
WORKDIR /usr/local/airflow

# Copy local Airflow files to image
COPY ./airflow-home /usr/local/airflow

# Sleep forever
CMD ["sleep",  "infinity"]

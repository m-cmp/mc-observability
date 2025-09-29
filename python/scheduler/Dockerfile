FROM python:3.11-slim
USER root

# MySQL Requirements
RUN apt-get update && \
    apt-get install -y build-essential  default-libmysqlclient-dev pkg-config python3-dev

# Python Requirements
COPY requirements.txt .
RUN pip3 install -r requirements.txt
RUN pip3 install connexion
RUN pip3 install "connexion[swagger-ui]"
RUN pip3 cache purge

RUN rm -rf /etc/localtime && \
    ln -s /usr/share/zoneinfo/Asia/Seoul /etc/localtime

RUN apt-get purge -y \
        python3-dev \
        default-libmysqlclient-dev \
        build-essential && \
    apt-get autoremove -y && \
    rm -rf /var/lib/apt/lists/* /var/cache/apt/archives/* && \
    apt-get clean

# Airflow Env Vars
ENV AIRFLOW_HOME='/usr/local/airflow'

# Set wd
WORKDIR /usr/local/airflow

# Copy local Airflow files to image
COPY ./airflow-home /usr/local/airflow

# Sleep forever
CMD sleep infinity

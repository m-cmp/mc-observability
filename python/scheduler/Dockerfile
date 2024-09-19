FROM python:3.9-slim
USER root

# MySQL Requirements
RUN apt-get update && apt-get install -y python3-dev default-libmysqlclient-dev build-essential pkg-config && apt-get clean

# Python Requirements
ADD requirements.txt .
RUN pip3 install -r requirements.txt
RUN pip3 install connexion
RUN pip3 install "connexion[swagger-ui]"
RUN pip3 cache purge
RUN rm -rf /etc/localtime && \
    ln -s /usr/share/zoneinfo/Asia/Seoul /etc/localtime

# Airflow Env Vars
ENV AIRFLOW_HOME='/usr/local/airflow'

# Set wd
WORKDIR /usr/local/airflow

# Sleep forever
CMD sleep infinity

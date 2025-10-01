FROM python:3.12-slim

WORKDIR /mc-insight

RUN apt-get update && apt-get install -y vim curl && apt-get clean && rm -rf /var/lib/apt/lists/*

COPY requirements.txt /mc-insight
COPY /external /mc-insight/external
RUN pip install -r requirements.txt
COPY . /mc-insight

EXPOSE 9001

COPY --chmod=755 entrypoint.sh /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]

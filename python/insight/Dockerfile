FROM python:3.9-slim

WORKDIR /sherlock

COPY requirements.txt /sherlock
RUN pip install -r requirements.txt
COPY . /sherlock

EXPOSE 9001

COPY entrypoint.sh /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]

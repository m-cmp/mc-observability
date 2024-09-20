FROM python:3.11-slim

WORKDIR /mc-insight

COPY requirements.txt /mc-insight
RUN pip install -r requirements.txt
COPY . /mc-insight

EXPOSE 9001

COPY --chmod=755 entrypoint.sh /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]

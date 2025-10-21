FROM grafana/grafana:11.5.1-ubuntu

USER root

RUN sed -i 's@archive.ubuntu.com@mirror.kakao.com@g' /etc/apt/sources.list
RUN apt-get update && apt-get install -y nginx \
    && rm -rf /var/lib/apt/lists/* /var/cache/apt/archives/* \
    && apt-get clean

COPY ./nginx.conf /etc/nginx/nginx.conf
RUN chown -R grafana /var/log/nginx
RUN chown -R grafana /var/lib/nginx

RUN mkdir -p /etc/grafana/provisioning/datasources
COPY ./run.sh /grafana/run.sh
RUN chmod 755 /grafana/run.sh

RUN mkdir -p /grafana/sslkey/
RUN openssl req -newkey rsa:2048 -nodes -keyout /grafana/sslkey/m-cmp_ca.key \
-x509 -days 3650 -out /grafana/sslkey/m-cmp_ca.crt \
-subj "/C=KR/ST=Seoul/L=Jung-gu/O=Innogrid/OU=R&D/CN=m-cmp CA/emailAddress=rnd-admin@innogrid.com" && \
openssl req -newkey rsa:2048 -nodes -keyout /grafana/sslkey/m-cmp.key \
-out /grafana/sslkey/m-cmp.csr -subj "/C=KR/ST=Seoul/L=Jung-gu/O=Innogrid/OU=R&D/CN=*.mcmp.com" && \
printf "subjectAltName=DNS:*.mcmp.com" > /grafana/sslkey/mcmp_extfile && \
openssl x509 -req -CAcreateserial -extfile /grafana/sslkey/mcmp_extfile \
-CA /grafana/sslkey/m-cmp_ca.crt -CAkey /grafana/sslkey/m-cmp_ca.key \
-in /grafana/sslkey/m-cmp.csr -out /grafana/sslkey/m-cmp.crt -days 3650
RUN chown -R grafana /grafana/sslkey

USER grafana

ENTRYPOINT ["/grafana/run.sh"]

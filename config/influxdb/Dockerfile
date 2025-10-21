FROM influxdb:1.11.8

COPY ./influxdb.conf /etc/influxdb/influxdb.conf

ENTRYPOINT ["/entrypoint.sh"]
CMD ["influxd"]

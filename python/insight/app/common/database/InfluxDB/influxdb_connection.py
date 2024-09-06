from opcode import cmp_op
from influxdb import InfluxDBClient, exceptions
import logging

from config.ConfigManager import read_influxdb_config

logger = logging.getLogger()


class InfluxDBConn:
    def __init__(self):
        self.influxdb_info = read_influxdb_config()
        self.client = self.create_client()

    def create_client(self):
        try:
            client = self.validate_db_credentials(host=self.influxdb_info['host'], port=self.influxdb_info['port'],
                                                  username=self.influxdb_info['username'], password=self.influxdb_info['password'],
                                                  database=self.influxdb_info['database'])
            return client
        except Exception as e:
            raise Exception(e)


    @staticmethod
    def validate_db_credentials(host, port, username, password, database):
        client = InfluxDBClient(host=host, port=port, username=username, password=password, database=database)

        try:
            client.query('SHOW DATABASES')
            return client

        except exceptions.InfluxDBClientError as e:
            raise Exception(f'Unable to create the InfluxDB client {e}')





if __name__ == '__main__':
    conn = InfluxDBConn().client
    print(conn)
    print(conn.query('show databases'))
    print(conn.get_list_database())
    vm_id = "3c28fd90-8688-4058-8dbf-6cba0d47de43"
    # result = conn.query(f'select mean("usage_idle") as "mean_usage_idle" from "cmp"."autogen"."cpu" where time > now() - 1d AND "uuid" = \'{vm_id}\' GROUP BY time(1h)')
    # print(result.raw)

    json_body = [
        {
            "measurement": "cpu",
            "tags": {
                "uuid": "o11y-test"
            },
            "time": "2024-09-10T00:00:00Z",
            "fields": {
                "usage_idle": 70
            }
        },
        {
            "measurement": "cpu",
            "tags": {
                "uuid": "o11y-test"
            },
            "time": "2024-09-10T01:00:00Z",
            "fields": {
                "usage_idle": 80
            }
        },
        {
            "measurement": "cpu",
            "tags": {
                "uuid": "o11y-test"
            },
            "time": "2024-09-10T02:00:00Z",
            "fields": {
                "usage_idle": 90
            }
        }
    ]
    a = conn.write_points(json_body)
    print(a)


    '''
    query_strig = 'select mean("usage_idle") as "mean_usage_idle" from "cmp"."autogen"."cpu" where time < now() and "uuid"="3c28fd90-8688-4058-8dbf-6cba0d47de43" group by "time(10m)" fill(null)'
    print(f'query_string: {query_strig}')
    result = conn.query('select mean("usage_idle") as "mean_usage_idle" from "cmp"."autogen"."cpu" where time < now() and "uuid"="3c28fd90-8688-4058-8dbf-6cba0d47de43" group by "time(10m)" fill(null)')
    print(result)
    '''
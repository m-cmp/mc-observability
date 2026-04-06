from influxdb import InfluxDBClient
from collections import defaultdict
from urllib.parse import urlparse
import warnings
import pandas as pd
from downsampling_utils.downsampling import weighted_moving_average, data_reduction
import requests

warnings.filterwarnings("ignore", category=FutureWarning)


class DataProcessor:
    def __init__(self, api_base_url: str, metric_info_list):
        self.api_base_url = api_base_url
        self.metric_info_list = metric_info_list
        self.headers = {"Content-Type": "application/json"}

    def process_measurements(self):
        influx_servers = self._get_influx_servers()
        vm_list = self._get_all_vms()

        # Group (nsId, mciId) by influx server id
        routing = defaultdict(set)
        for vm in vm_list:
            influx_seq = vm.get("influx_seq")
            ns_id = vm.get("ns_id")
            mci_id = vm.get("mci_id")
            if influx_seq and ns_id and mci_id:
                routing[influx_seq].add((ns_id, mci_id))

        # Process each InfluxDB server with its assigned ns/mci pairs
        for server in influx_servers:
            server_id = server.get("id")
            ns_mci_pairs = routing.get(server_id, set())
            if not ns_mci_pairs:
                print(f"InfluxDB id={server_id} has no assigned VMs, skipping.")
                continue

            parsed = urlparse(server["url"])
            host = parsed.hostname
            port = parsed.port or 8086
            username = server.get("username", "")
            password = server.get("password", "")
            src_db = server.get("database", "mc-observability")

            read_client = InfluxDBClient(host=host, port=port, username=username, password=password, database=src_db)
            write_client = InfluxDBClient(host=host, port=port, username=username, password=password, database="downsampling")
            write_client.create_database("downsampling")

            print(f"Processing InfluxDB id={server_id} url={server['url']} (read={src_db}, write=downsampling, {len(ns_mci_pairs)} ns/mci pairs)")
            self._process_server(read_client, write_client, ns_mci_pairs)
            read_client.close()
            write_client.close()

    def _get_influx_servers(self):
        api_url = self.api_base_url + "/monitoring/influxdb"
        resp = requests.get(api_url, headers=self.headers)
        resp.raise_for_status()
        return resp.json().get("data", [])

    def _get_all_vms(self):
        api_url = self.api_base_url + "/monitoring/vm"
        resp = requests.get(api_url, headers=self.headers)
        resp.raise_for_status()
        return resp.json().get("data", [])

    def _process_server(self, read_client: InfluxDBClient, write_client: InfluxDBClient, ns_mci_pairs: set):
        for metric_info in self.metric_info_list:
            measurement = metric_info["measurement"]
            for field in metric_info["fields"]:
                if field["field_type"] not in ["integer", "float"]:
                    continue

                for ns_id, mci_id in ns_mci_pairs:
                    metric_data = self._load_metric_data(
                        client=read_client,
                        measurement=measurement,
                        field=field["field_key"],
                        ns_id=ns_id,
                        mci_id=mci_id,
                    )
                    if not metric_data:
                        continue

                    for metric_entry in metric_data:
                        try:
                            tags = metric_entry["tags"]
                            result_df = self.downsample_and_reduce(
                                metric_data=metric_entry["values"]
                            )
                            self._save_to_influx(
                                write_client, measurement, result_df, tags=tags, field=field["field_key"]
                            )
                        except Exception as e_msg:
                            print(
                                f"Error processing {measurement}/{field['field_key']} "
                                f"ns={ns_id} mci={mci_id}: {e_msg}"
                            )

            print(f"{measurement} done. ({read_client._host}:{read_client._port})")

    @staticmethod
    def _load_metric_data(client: InfluxDBClient, measurement: str, field: str, ns_id: str, mci_id: str):
        query = (
            f'SELECT MEAN("{field}") AS "y" FROM "{measurement}" '
            f"WHERE time > now() - 1h "
            f"AND \"ns_id\" = '{ns_id}' AND \"mci_id\" = '{mci_id}' "
            f'GROUP BY time(1m), "ns_id", "mci_id", "target_id"'
        )
        result = client.query(query)

        metric_data = []
        for (_, tags), rows in result.items():
            if not tags:
                continue
            values = [{"time": row["time"], "y": row["y"]} for row in rows]
            metric_data.append({"tags": tags, "values": values})

        return metric_data

    def downsample_and_reduce(self, metric_data):
        metric_data_df = pd.DataFrame(metric_data, columns=["time", "y"])
        metric_data_df.dropna(subset=["y"], inplace=True)
        metric_data_df.sort_values(by="time", inplace=True)
        metric_data_df.reset_index(drop=True, inplace=True)

        wma_df = weighted_moving_average(metric_data_df, ["y"])
        reduced_df = data_reduction(wma_df, ["y"], cut_size=6)
        reduced_df = pd.concat(
            [reduced_df, metric_data_df.loc[[0], ["time", "y"]]], ignore_index=True
        )

        return reduced_df

    @staticmethod
    def _save_to_influx(client, measurement, result_df, tags, field):
        try:
            data_points = DataProcessor._df_to_influx_points(result_df, measurement, tags, field)
            client.write_points(data_points)
        except Exception as msg:
            print(f"DB ERROR ({client._host}:{client._port})")
            print(msg)

    @staticmethod
    def _df_to_influx_points(df, measurement_name, tags, field):
        points = []
        for _, row in df.iterrows():
            point = {
                "measurement": measurement_name,
                "time": row["time"],
                "tags": {
                    "ns_id": tags["ns_id"],
                    "mci_id": tags["mci_id"],
                    "target_id": tags["target_id"],
                },
                "fields": {field: row["y"]},
            }

            points.append(point)
        return points

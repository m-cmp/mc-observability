#!/bin/sh

export RUN_PATH=`dirname $0`

docker -v
if [ $? -ne 0 ]; then
  sudo mkdir -p /etc/docker/
  echo  '{
        "log-driver": "syslog",
        "log-opts": {
                "syslog-format": "rfc3164",
                "tag": "{{.Name}}"
        }
}
' | sudo tee /etc/docker/daemon.json

  # Install Docker CE
  sudo apt-get install -y apt-transport-https ca-certificates curl gnupg-agent software-properties-common
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -
  sudo add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable"
  sudo apt-get update
  sudo apt-get install -y docker-ce docker-ce-cli docker-compose-plugin
fi

cd $RUN_PATH/../
cp .env.sample .env

sudo mkdir -p /docker/opensearch
sudo mkdir -p /docker/kapacitor_data
sudo chown -R 1000:1000 /docker/opensearch
sudo chown -R 999:999 /docker/kapacitor_data
sudo docker compose up -d
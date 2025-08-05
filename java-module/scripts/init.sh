#!/bin/sh

cd $HOME

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
  sudo apt-get update
  sudo apt-get install ca-certificates curl
  sudo install -m 0755 -d /etc/apt/keyrings
  sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
  sudo chmod a+r /etc/apt/keyrings/docker.asc

  # Add the repository to Apt sources:
  echo \
    "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu \
    $(. /etc/os-release && echo "${UBUNTU_CODENAME:-$VERSION_CODENAME}") stable" | \
    sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
  sudo apt-get update
  sudo apt-get install docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
fi

git --version
if [ $? -ne 0 ]; then
  sudo apt-get install -y git
fi

rm -rf mc-observability
git clone https://github.com/m-cmp/mc-observability.git

cd mc-observability/java-module/mc-o11y-agent/

cat <<EOF > .env
NS_ID=$2
MCI_ID=$3
TARGET_ID=$4
TUMBLEBUG_URL=$5
TUMBLEBUG_ID=$6
TUMBLEBUG_PW=$7
SPIDER_URL=$8
SPIDER_ID=$9
SPIDER_PW=${10}
SPIDER_MONITORING_INFLUXDB_URL=http://$1:8086
DATABASE_HOST=$1
DATABASE_NAME=mc-observability
DATABASE_ID=mc-agent
DATABASE_PW=mc-agent
EOF

sudo mkdir -p /var/log/mc-observability/
sudo docker compose up -d

#!/bin/sh

cd $HOME

docker -v
if [ $? -ne 0 ]; then
  # Install Docker CE
  sudo apt-get install -y apt-transport-https ca-certificates curl gnupg-agent software-properties-common
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -
  sudo add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable"
  sudo apt-get update
  sudo apt-get install -y docker-ce docker-ce-cli docker-compose-plugin
fi

git --version
if [ $? -ne 0 ]; then
  sudo apt-get install -y git
fi

git clone https://github.com/m-cmp/mc-observability.git

cd mc-observability/java-module/

cat <<EOF > .env
NS_ID=$2
MCI_ID=$3
TARGET_ID=$4
DATABASE_HOST=$1
DATABASE_NAME=mc-observability
DATABASE_ID=mc-agent
DATABASE_PW=mc-agent
EOF

sudo docker compose up -d
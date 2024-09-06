#!/bin/sh

cd $HOME

docker -v
if [ $? -ne 0 ]; then
  # Install Docker CE
  sudo apt-get install -y apt-transport-https ca-certificates curl gnupg-agent software-properties-common
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -
  sudo add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable"
  sudo apt-get update
  sudo apt-get install -y docker-ce docker-ce-cli docker-compose
fi

cat <<EOF > mc-o11y-agent.conf
JAVA_OPTS="-Dspring.profiles.active=prd -Dmc-o11y.ns-id=$2 -Dmc-o11y.mci-id=$3 -Dmc-o11y.target-id=$4"
EOF

cat <<EOF > application-prd.yaml
spring:
  datasource:
    driver-class-name: org.mariadb.jdbc.Driver
    url: jdbc:mariadb://$1:3306/mc_observability?useUnicode=true&characterEncoding=utf8&serverTimeZone=Asia/Seoul
    username: mc-agent
    password: mc-agent
EOF


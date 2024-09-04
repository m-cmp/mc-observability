#!/bin/sh

cd ..
chmod +x gradlew

./gradlew build

cd $HOME

if [ ! -d mcmp ]; then
  mkdir mcmp
  echo "Create mcmp dir"
fi

wget https://dl.influxdata.com/telegraf/releases/telegraf-1.26.1_linux_amd64.tar.gz
tar xfpz telegraf-1.26.1_linux_amd64.tar.gz ./telegraf-1.26.1/usr/bin/telegraf --strip-components 4 && mv telegraf mc-observability-agent-collector
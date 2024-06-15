#!/bin/bash

RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m'

function read_password() {
  PW_TMP=
  local password=""
  local char

  stty -echo
  while IFS= read -r -s -n1 char; do
    if [[ $char == $'\0' ]]; then
      break
    fi
    if [[ $char == $'\177' ]]; then
      if [ ${#password} -gt 0 ]; then
        password=${password%?}
        echo -ne "\b \b"
      fi
    else
      password+=$char
      echo -n "*"
    fi
  done
  stty echo
  PW_TMP=$password
}

function read_ip() {
  local pattern="^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
  while true; do
    echo -en "${GREEN}IP            : ${NC}"
    read IP
    if echo "$IP" | grep -Eq "$pattern" ; then
      break
    else
      echo "Invaild IP format"
    fi
  done
}

function read_datasource() {
  local pattern="^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?):[0-9]{1,5}$"
  while true; do
    echo -en "${GREEN}Datasource          : ${NC}"
    read DATASOURCE

    if echo "$DATASOURCE" | grep -Eq ".*,$" ; then
      echo "$DATASOURCE is Invaild Datasource format"
      continue
    fi
    
    IFS=","
    read -r -a array <<< "$DATASOURCE"

    local isValid=true
    for element in "${array[@]}"; do
      if echo "$element" | grep -Eq "$pattern" ; then
        empty_line=''
      else
        echo "$element is Invaild Datasource format"
        isValid=false
      fi
    done
    if $isValid ; then
      break
    fi
  done
}

if command -v sshpass >/dev/null 2>&1; then
  empty_line=''
else
  echo "This script is require 'sshpass'"
  exit 0
fi

clear
echo -e "${GREEN}[SSH connection info]${NC}"
read_ip
echo -en "${GREEN}Port [Default : 22]     : ${NC}"
read PORT
PORT=${PORT:-22}
echo -en "${GREEN}Username [Default : root] : ${NC}"
read USERNAME
USERNAME=${USERNAME:-root}
echo -en "${GREEN}Password          : ${NC}"
read_password
PASSWORD=$PW_TMP
echo

if echo > /dev/tcp/$IP/$PORT >/dev/null; then
  empty_line=''
else
  exit 0
fi

echo
echo -e "${GREEN}[MariaDB connection info]${NC}"
echo -e "Datasource is IP:Port"
echo -e " - single   datasource : 127.0.0.1:3306"
echo -e " - sequential datasource : 127.0.0.1:3306,127.0.0.2:3306"
read_datasource
echo -en "${GREEN}Database [Default : mc_agent] : ${NC}"
read MARIA_DATABASE
MARIA_DATABASE=${MARIA_DATABASE:-mc_agent}
echo -en "${GREEN}Username [Default : mc-agent] : ${NC}"
read MARIA_USERNAME
MARIA_USERNAME=${MARIA_USERNAME:-mc-agent}
echo -en "${GREEN}Password [Default : mc-agent] : ${NC}"
read_password
MARIA_PASSWORD=${PW_TMP:-mc-agent}
echo

echo -e "${GREEN}Select Installation package type${NC}"
select OS in "rpm" "deb"; do
  case $OS in
    "rpm")
      OS=$OS
      URL='http://210.207.104.211:8081/repository/rpm-hosted/mc-agent/v0.2.0/amd64/mc-agent-v0.2.0-1.x86_64.rpm'
      FILE='mc-agent-v0.2.0-1.x86_64.rpm'
      break
    ;;
    "deb")
      OS=$OS
      URL='http://210.207.104.211:8081/repository/deb-hosted/pool/m/mc-agent/mc-agent_0v0.2.0-2_amd64.deb'
      FILE='mc-agent_0v0.2.0-2_amd64.deb'
      break
    ;;
  esac
done

sshpass -p "${PASSWORD}" ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null ${USERNAME}@${IP} curl ${URL} -o ~/${FILE}

case $OS in
  "rpm")
    sshpass -p "${PASSWORD}" ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null ${USERNAME}@${IP} rpm -Uvh ~/${FILE}
  ;;
  "deb")
    sshpass -p "${PASSWORD}" ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null ${USERNAME}@${IP} dpkg -i ~/${FILE}
  ;;
esac

sshpass -p "${PASSWORD}" ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null ${USERNAME}@${IP} mc-agent setup -d ${DATASOURCE}/${MARIA_DATABASE} -u ${MARIA_USERNAME} -p ${MARIA_PASSWORD}
sshpass -p "${PASSWORD}" ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null ${USERNAME}@${IP} systemctl enable mc-agent
sshpass -p "${PASSWORD}" ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null ${USERNAME}@${IP} systemctl start mc-agent

exit 0

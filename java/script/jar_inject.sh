#!/bin/bash

cd ../target || exit

mv mc-agent*.jar mc-agent

sed -i '2i\
[[ $1 = "-v" || $1 = "--version" ]] && echo "Application version 0.2.0" && exit 0\
function help() {\
  echo "List of Commands:"\
  echo "setup [Options]    mc-agent Require setup command"\
  echo "init               if mc-agent uuid duplicate resolve command (using openssl generate new uuid)"\
  echo ""\
  echo ""\
  echo "Options:"\
  echo "  -h, --help          show this help message and exit"\
  echo "  -v, --version       show mc-agent version and exit"\
  echo ""\
  echo "setup Command Options:"\
  echo "  -d, --datasource    MariaDB jdbc datasource (Required)"\
  echo "                      e.g single  source) 127.0.0.1:3306/mc-agent"\
  echo "                      e.g cluster source) 127.0.0.1:3306,127.0.0.2:3306/mc-agent"\
  echo "                      refer) finally setting is jdbc:mariadb:sequential://\${HOST}?useUnicode=true&characterEncoding=utf8&serverTimeZone=Asia/Seoul"\
  echo ""\
  echo "  -u, --username      MariaDB username (Required)"\
  echo ""\
  echo "  -p, --password      MariaDB password (Required)"\
}\
if [[ $1 = "-h" || $1 = "--help" ]] ; then\
  help\
  exit 0\
fi\
\
function argParse() {\
  for ((i=2;i<=$#;i+=2))\
  do\
    KEY=${!i}\
    case ${KEY} in\
      -d|-u|-p|--datasource|--username|--password)\
        ;;\
      *)\
        echo "setup Options Check"\
        help\
        exit 0\
        ;;\
    esac\
\
    VALUE=$((i+1))\
    VALUE=${!VALUE}\
    case ${KEY} in\
      -d|--datasource)\
        DATABASE_URL="jdbc:mariadb:sequential://${VALUE}?useUnicode=true&characterEncoding=utf8&serverTimeZone=Asia/Seoul"\
        ;;\
      -u|--username)\
        USERNAME=${VALUE}\
        ;;\
      -p|--password)\
        PASSWORD=${VALUE}\
        ;;\
      *)\
        echo "setup Options Check"\
        help\
        exit 0\
        ;;\
    esac\
  done\
  \
  touch application-prd.yml\
\
  SPACE="  "\
  echo "spring:" > application-prd.yml\
  echo "${SPACE}datasource:" >> application-prd.yml\
  echo "${SPACE}${SPACE}driver-class-name: org.mariadb.jdbc.Driver" >> application-prd.yml\
  echo "${SPACE}${SPACE}url: ${DATABASE_URL}" >> application-prd.yml\
  echo "${SPACE}${SPACE}username: ${USERNAME}" >> application-prd.yml\
  echo "${SPACE}${SPACE}password: ${PASSWORD}" >> application-prd.yml\
  \
  mv application-prd.yml /etc/mc-agent/\
  \
  chown mc-agent:mc-agent /etc/mc-agent/application-prd.yml\
}\
\
if [[ $1 = "setup" ]] ; then\
  if [[ $# = 1 ]] ; then\
    echo "setup Options Check"\
    help\
    exit 0\
  else\
    argParse $@\
    exit 0\
  fi\
fi\
\
if [[ $1 = "init" ]] ; then\
  touch /etc/mc-agent/uuid\
  uuid=$(openssl rand -hex 16)\
  echo ${uuid:0:8}-${uuid:8:4}-${uuid:12:4}-${uuid:16:4}-${uuid:20:12} > /etc/mc-agent/uuid\
  chown -R -L mc-agent:mc-agent /etc/mc-agent\
  exit 0\
fi\
' mc-agent

exit 0
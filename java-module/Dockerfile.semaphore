FROM semaphoreui/semaphore:v2.12.17

USER root

RUN apk --no-cache add tzdata
RUN echo "Asia/Seoul" >  /etc/timezone
RUN cp -f /usr/share/zoneinfo/Asia/Seoul /etc/localtime

RUN apk add sudo
RUN apk add openssh
RUN apk add iproute2

RUN echo "semaphore ALL = NOPASSWD: ALL" >> /etc/sudoers

RUN mkdir -p /root/.ssh
RUN chmod 2700 /root/.ssh
RUN ln -s /dev/null /root/.ssh/known_hosts

FROM openjdk:8

LABEL MAINTAINER="MAIF Team <maif@maif.fr>"

ADD nio.zip /opt
RUN unzip -q /opt/nio.zip -d /opt
RUN rm /opt/nio.zip
RUN mv /opt/nio /opt/backend

WORKDIR /opt/backend

EXPOSE 9000

RUN ["mkdir", "-p", "/data"]

VOLUME ["/data"]

ENTRYPOINT /opt/backend/bin/nio-server -Dlogger.file=./conf/prod-logger.xml  -Dcluster.akka.remote.netty.tcp.hostname="$(eval "awk 'END{print $1}' /etc/hosts")"  -Dcluster.akka.remote.netty.tcp.bind-hostname="$(eval "awk 'END{print $1}' /etc/hosts")"

CMD []

ENV APP_NAME Nio
ENV APP_VERSION 1.0.0-SNAPSHOT
ENV HTTP_PORT 9000
ENV APPLICATION_SECRET 123456

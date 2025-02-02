FROM maven:3.9.9-amazoncorretto-21 AS webhook-router

RUN yum install -y git &&\
    yum clean all &&\
    rm -rf /var/cache/yum

ADD "https://raw.githubusercontent.com/javier-godoy/webhook-router/refs/heads/master/pom.xml" skipcache
RUN curl -O https://raw.githubusercontent.com/javier-godoy/webhook-router/refs/heads/master/pom.xml && mvn dependency:go-offline

ADD "https://api.github.com/repos/javier-godoy/webhook-router/branches/master" skipcache
RUN git clone https://github.com/javier-godoy/webhook-router.git

WORKDIR /webhook-router
RUN mvn package && rm -rf ~/.m2

FROM amazoncorretto:21.0.2-alpine3.19

RUN apk add --no-cache inotify-tools

COPY --from=webhook-router /webhook-router/target/webhook-router.jar /webhook-router.jar
COPY webhook-spool.sh /webhook-spool.sh
CMD /webhook-spool.sh
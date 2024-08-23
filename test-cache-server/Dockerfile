FROM openjdk:17

ENV SERVICE_HOME=/etc/test-cache-server \
    LANG=C.UTF-8

WORKDIR $SERVICE_HOME

COPY ./target/test-cache-server.jar ./

EXPOSE 8080

CMD ["java", "-jar", "test-cache-server.jar"]

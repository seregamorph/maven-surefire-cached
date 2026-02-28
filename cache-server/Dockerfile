FROM eclipse-temurin:17-jdk

ENV SERVICE_HOME=/etc/cache-server \
    LANG=C.UTF-8

WORKDIR $SERVICE_HOME

COPY ./target/cache-server.jar ./

EXPOSE 8080

CMD ["java", "-jar", "cache-server.jar"]

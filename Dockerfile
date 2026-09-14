FROM maven:3.9.16-eclipse-temurin-21 AS build
WORKDIR /build

COPY pom.xml ./
COPY src ./src
RUN mvn --batch-mode --no-transfer-progress clean package

FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

RUN groupadd --gid 10001 catalog \
    && useradd --uid 10001 --gid catalog --no-create-home --shell /usr/sbin/nologin catalog \
    && mkdir -p /opt/oracle/wallet

COPY --from=build --chown=catalog:catalog /build/target/*.jar /app/app.jar

USER catalog
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

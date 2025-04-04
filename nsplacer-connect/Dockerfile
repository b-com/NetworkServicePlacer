FROM maven:3.9.9-eclipse-temurin-11-alpine AS builder

WORKDIR /src

COPY pom.xml pom.xml

RUN set -ex \
 && mvn dependency:go-offline

COPY src src

RUN set -ex \
 && mvn package -DskipTests

FROM eclipse-temurin:11.0.26_4-jre-alpine-3.21

WORKDIR /app

COPY --from=builder /src/target/nsplacer-connect.jar /app/nsplacer-connect.jar

ENTRYPOINT ["java", "-jar", "./nsplacer-connect.jar"]

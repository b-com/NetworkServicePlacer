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

COPY --from=builder /src/target/nsplacer.jar /app/nsplacer.jar
COPY res res
COPY zoo-topologies zoo-topologies

CMD ["java", "-jar", "./nsplacer.jar"]

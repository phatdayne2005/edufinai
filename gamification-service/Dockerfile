#
# Multi-stage build:
# 1. Maven builder downloads dependencies & packages the jar.
# 2. Lightweight runtime image copies only the final artifact.
#

FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /workspace

# Copy pom first to leverage Docker cache for dependencies
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

# Copy the rest of the source code and build
COPY src src
RUN mvn -q -B clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

ENV SPRING_PROFILES_ACTIVE=docker

COPY --from=builder /workspace/target/*.jar /app/app.jar

EXPOSE 8203

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
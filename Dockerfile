# syntax=docker/dockerfile:1
# ---- Build stage: compile and package the Spring Boot jar -------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Resolve dependencies first so they are cached unless pom.xml changes.
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B clean package -DskipTests

# ---- Run stage: slim JRE with just the jar ----------------------------------
FROM eclipse-temurin:21-jre AS run
WORKDIR /app
COPY --from=build /app/target/ehr-api-*.jar app.jar
EXPOSE 8081

# Sandbox defaults: in-memory H2, the API as its own identity provider (local
# mode) with demo users seeded, and local billing. Override any of these envs
# for higher environments.
ENV SPRING_PROFILES_ACTIVE=h2 \
    EHR_SECURITY_MODE=local \
    EHR_SEED_DEMO_USERS=true \
    EHR_BILLING_MODE=local

ENTRYPOINT ["java", "-jar", "app.jar"]

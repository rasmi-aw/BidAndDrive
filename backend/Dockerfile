# syntax=docker/dockerfile:1

# --- build: package the Spring Boot jar (tests run in CI: they need Docker for Testcontainers)
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build
COPY . .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -Dmaven.test.skip=true package

# --- runtime: JRE only, non-root
FROM eclipse-temurin:25-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /build/auction-app/target/auction-app-*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]

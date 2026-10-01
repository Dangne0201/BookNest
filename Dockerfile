FROM maven:3.9.16-eclipse-temurin-17-alpine AS build

WORKDIR /workspace

COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw

COPY src src
RUN ./mvnw --no-transfer-progress -DskipTests package \
    && mkdir -p /app \
    && cp target/booknest-0.0.1-SNAPSHOT.jar /app/app.jar

FROM eclipse-temurin:17-jre-alpine AS runtime

RUN addgroup -S spring && adduser -S spring -G spring
WORKDIR /app

COPY --from=build --chown=spring:spring /app/app.jar app.jar

USER spring:spring
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

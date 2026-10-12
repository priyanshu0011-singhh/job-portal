FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace
COPY pom.xml ./
COPY src src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app
RUN addgroup -S -g 10001 app && adduser -S -u 10001 -G app app \
    && mkdir -p /app/uploads/resumes \
    && chown -R app:app /app
COPY --from=build --chown=app:app /workspace/target/JobPortal-0.0.1-SNAPSHOT.jar /app/app.jar

USER 10001:10001
EXPOSE 8099
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
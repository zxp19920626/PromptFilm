FROM maven:3.9.9-eclipse-temurin-11 AS build

WORKDIR /workspace
COPY pom.xml ./
COPY src ./src
RUN mvn --batch-mode --no-transfer-progress -DskipTests package

FROM maven:3.9.9-eclipse-temurin-11 AS runtime

RUN useradd --create-home --uid 10001 --shell /usr/sbin/nologin promptfilm
WORKDIR /app
COPY --from=build --chown=promptfilm:promptfilm \
    /workspace/target/prompt-film-backend-0.0.1-SNAPSHOT.jar /app/prompt-film-backend.jar

USER promptfilm
EXPOSE 18080

ENTRYPOINT ["java", "-jar", "/app/prompt-film-backend.jar"]

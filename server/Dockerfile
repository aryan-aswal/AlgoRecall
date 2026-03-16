# Stage 1: Build the application with Java 17 (matches pom.xml)
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Copy only pom first to leverage Docker layer cache for dependencies
COPY pom.xml ./
RUN mvn -B dependency:go-offline

# Copy source and build the fat jar
COPY src ./src
RUN mvn -B clean package -DskipTests

# Stage 2: Run as non-root on a slim JRE image
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

RUN groupadd --system app && useradd --system --gid app --home-dir /app app
COPY --from=build /workspace/target/*.jar /app/app.jar
RUN chown app:app /app/app.jar

USER app
EXPOSE 8080

# Tuned defaults for containers; can be overridden at runtime
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:InitialRAMPercentage=25.0 -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

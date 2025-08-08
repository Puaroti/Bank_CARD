# Multi-stage build for Spring Boot app (Java 21)

# ====== Build stage ======
FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /app

# Copy sources and build
ENV MAVEN_OPTS="-Dmaven.wagon.http.retryHandler.count=5 -Dhttp.keepAlive=false"
COPY pom.xml ./
COPY src ./src
RUN mvn -B -U -DskipTests \
    -Dmaven.wagon.http.retryHandler.count=5 \
    -Dhttp.keepAlive=false \
    -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn \
    package

# ====== Runtime stage ======
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the shaded boot jar
COPY --from=build /app/target/*.jar /app/app.jar

EXPOSE 8080
# Allow overrides from compose
ENV JAVA_TOOL_OPTIONS=""

ENTRYPOINT ["java","-jar","/app/app.jar"]

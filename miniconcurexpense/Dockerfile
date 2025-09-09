# build stage
FROM maven:3.9.4-eclipse-temurin-21 AS builder
WORKDIR /workspace
COPY pom.xml mvnw ./
COPY .mvn .mvn
COPY src src
# use mvn (maven image has mvn) - skip tests for quick builds
RUN mvn -DskipTests package

# runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /workspace/target/*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]

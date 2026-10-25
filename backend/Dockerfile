FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY .mvn .mvn
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src src
RUN mvn -B package
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/target/banking-academy-1.0.0.jar app.jar
EXPOSE 18080
ENTRYPOINT ["java", "-jar", "app.jar"]

FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/BOT-EST-POBRITIE-1.0-SNAPSHOT.jar app.jar
CMD ["java", "-jar", "app.jar"]
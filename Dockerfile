FROM openjdk:21-jdk-slim
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests || mvn clean package -DskipTests
CMD ["java", "-jar", "target/BOT-EST-POBRITIE-1.0-SNAPSHOT.jar"]
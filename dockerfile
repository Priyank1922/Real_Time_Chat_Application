FROM maven:3.9-eclipse-temurin-25

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

EXPOSE 8080

CMD ["java", "-jar", "target/realtime-chat-backend-1.0.0.jar"]
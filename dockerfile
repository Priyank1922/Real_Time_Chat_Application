FROM eclipse-temurin:25-jdk

WORKDIR /app

COPY . .

RUN ./mvnw clean package -DskipTests

EXPOSE 8080

CMD ["java", "-jar", "target/realtime-chat-backend-1.0.0.jar"]
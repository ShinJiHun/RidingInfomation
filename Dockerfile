RUN apt-get update && apt-get install -y mysql-client
FROM openjdk:17-jdk-slim
COPY RidingInformation.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
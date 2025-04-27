# RidingInformation.jar용
FROM openjdk:17-jdk-slim
VOLUME /tmp
COPY build/libs/RidingInformation.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]


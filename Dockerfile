FROM openjdk:8-jdk-alpine
EXPOSE 8080
COPY target/*.jar demo.jar
CMD java -jar /demo.jar
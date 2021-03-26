FROM alpine:3.11
RUN apk add openjdk11 && java -version
EXPOSE 8080
RUN serco.crt serco.crt 
COPY target/*.jar demo.jar
CMD java -jar /demo.jar

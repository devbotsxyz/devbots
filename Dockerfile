FROM amazoncorretto:11-alpine

RUN mkdir /app
WORKDIR /app
COPY target/devbots.jar .

EXPOSE 8080

CMD java -jar devbots.jar

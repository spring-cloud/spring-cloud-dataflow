FROM springcloud/openjdk:latest

ARG JAR_FILE

ADD target/${JAR_FILE} spring-cloud-skipper-server.jar

ENTRYPOINT exec java -Djava.security.egd=file:/dev/./urandom -jar /spring-cloud-skipper-server.jar

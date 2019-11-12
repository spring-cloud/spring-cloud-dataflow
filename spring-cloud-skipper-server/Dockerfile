FROM springcloud/openjdk:2.0.0.RELEASE
ENV LANG=C.UTF-8
ARG JAR_FILE
ADD target/${JAR_FILE} spring-cloud-skipper-server.jar
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/spring-cloud-skipper-server.jar"]

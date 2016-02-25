FROM anapsix/alpine-java:8
VOLUME /tmp
ADD *.jar app-launcher.jar
RUN bash -c 'touch /app-launcher.jar'
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app-launcher.jar"]

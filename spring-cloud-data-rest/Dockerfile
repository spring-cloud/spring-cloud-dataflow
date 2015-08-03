FROM java:8
VOLUME /tmp
ADD target/spring-cloud-data-rest-1.0.0.BUILD-SNAPSHOT.jar data-admin.jar
RUN bash -c 'touch /data-admin.jar'
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/data-admin.jar"]

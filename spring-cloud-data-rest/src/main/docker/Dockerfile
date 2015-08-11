FROM java:8
VOLUME /tmp
ADD *-exec.jar  data-admin.jar
RUN bash -c 'touch /data-admin.jar'
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/data-admin.jar"]

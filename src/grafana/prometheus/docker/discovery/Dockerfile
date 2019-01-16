FROM openjdk:8-jdk-alpine
ENV SCDF_HOST_IP "127.0.0.1"
VOLUME /tmp
COPY ./spring-cloud-dataflow-prometheus-service-discovery-0.0.1-SNAPSHOT.jar service-discovery.jar
ENTRYPOINT ["java", \
   "-Djava.security.egd=file:/dev/./urandom", \
   "-Dlogging.path=/var/log/service-discovery", \
   "-jar", \
   "/service-discovery.jar", \
   "--server.port=8181", \
   "--metrics.prometheus.target.discovery.url=http://${SCDF_HOST_IP}:9393/runtime/apps", \
   "--metrics.prometheus.target.file.path=/tmp/scdf-targets/targets.json", \
   "--metrics.prometheus.target.refresh.rate=1000", \
   "--metrics.prometheus.target.mode=local" \
 ]

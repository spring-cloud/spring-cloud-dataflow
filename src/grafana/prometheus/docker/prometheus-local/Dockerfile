FROM prom/prometheus:v2.20.1
COPY prometheus-local-file.yml /etc/prometheus-local-file.yml
CMD  [ "--config.file=/etc/prometheus-local-file.yml", \
       "--storage.tsdb.path=/prometheus", \
       "--web.console.libraries=/usr/share/prometheus/console_libraries", \
       "--web.console.templates=/usr/share/prometheus/consoles" ]


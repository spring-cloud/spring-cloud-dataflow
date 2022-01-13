FROM grafana/grafana:8.0.2
ADD ./provisioning /etc/grafana/provisioning
ADD ./config.ini /etc/grafana/config.ini
ADD ./dashboards /var/lib/grafana/dashboards
ENV GF_INSTALL_PLUGINS digrich-bubblechart-panel,savantly-heatmap-panel,grafana-piechart-panel,jdbranham-diagram-panel

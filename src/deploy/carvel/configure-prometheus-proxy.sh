#!/usr/bin/env bash
function set_properties() {
    PREFIX=$1
    yq "${PREFIX}.micrometer.prometheus.rsocket.host=\"$HOST\"" -i ./scdf-values.yml
    yq "${PREFIX}.management.metrics.export.prometheus.pushgateway.base-url=\"http://$HOST:$PORT\"" -i ./scdf-values.yml
    yq "${PREFIX}.management.metrics.export.prometheus.pushgateway.enabled=true" -i ./scdf-values.yml
    yq "${PREFIX}.management.metrics.export.prometheus.pushgateway.shutdown-operation=\"PUSH\"" -i ./scdf-values.yml
    yq "${PREFIX}.management.metrics.export.prometheus.step=\"$STEP\"" -i ./scdf-values.yml
}
if [ "$2" = "" ]; then
    echo "Usage is: <host> <port> [step]"
    echo "Where <step> is the frequency of published metrics. Default is 10s"
    exit 1
fi
HOST=$1
PORT=$2
if [ "$3" != "" ]; then
    STEP=$3
else
    STEP=10s
fi

set_properties ".scdf.server.config"
set_properties ".scdf.server.config.spring.cloud.dataflow.task"
set_properties ".scdf.skipper.config"
set_properties ".scdf.skipper.config.spring.cloud.skipper.server.platform.kubernetes.accounts.default"

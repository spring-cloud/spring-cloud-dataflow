#!/usr/bin/env bash
if [ -n "$BASH_SOURCE" ]; then
  SCDIR="$(readlink -f "${BASH_SOURCE[0]}")"
elif [ -n "$ZSH_VERSION" ]; then
  setopt function_argzero
  SCDIR="${(%):-%N}"
elif eval '[[ -n ${.sh.file} ]]' 2>/dev/null; then
  eval 'SCDIR=${.sh.file}'
else
  echo 1>&2 "Unsupported shell. Please use bash, ksh93 or zsh."
  exit 2
fi
SCDIR="$(dirname "$SCDIR")"

bold="\033[1m"
dim="\033[2m"
end="\033[0m"

start_time=$(date +%s)
K8S=$(realpath "$SCDIR/../kubernetes")
if [ ! -d "$K8S" ]; then
  K8S=$(realpath "$SCDIR/../../kubernetes")
fi

$SCDIR/prepare-local-namespace.sh prometheus prometheus
kubectl create serviceaccount prometheus-rsocket-proxy --namespace prometheus

kubectl create --namespace prometheus -f $K8S/prometheus/
kubectl create --namespace prometheus -f $K8S/prometheus-proxy/
kubectl create --namespace prometheus -f $K8S/grafana/

kubectl rollout status deployment --namespace prometheus grafana
kubectl rollout status deployment --namespace prometheus prometheus
kubectl rollout status deployment --namespace prometheus prometheus-rsocket-proxy
GRAFANA_HOST=$(kubectl get --namespace prometheus services grafana | grep -F grafana | awk '{ print $3 }')
echo "Set dashboard url=$GRAFANA_HOST:3000"
yq ".scdf.server.metrics.dashboard.url=\"http://$GRAFANA_HOST:3000\"" -i ./scdf-values.yml
yq ".scdf.feature.monitoring.grafana.enabled=true" -i ./scdf-values.yml
yq ".scdf.feature.monitoring.prometheusRsocketProxy.enabled=true" -i ./scdf-values.yml

$SCDIR/configure-prometheus-proxy.sh prometheus-rsocket-proxy.prometheus 7001

end_time=$(date +%s)
elapsed=$((end_time - start_time))
echo -e "Deployed Prometheus, Prometheus proxy and Grafana in ${bold}$elapsed${end} seconds"

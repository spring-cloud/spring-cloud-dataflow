#!/usr/bin/env bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
start_time=$(date +%s)
K8S=$(realpath $SCDIR/../../kubernetes)

kubectl create --namespace "$NS" -f $K8S/prometheus/prometheus-clusterroles.yaml
yq ".subjects[0].namespace=\"$NS\"" $K8S/prometheus/prometheus-clusterrolebinding.yaml | kubectl create --namespace "$NS" -f -
yq ".metadata.namespace=\"$NS\"" $K8S/prometheus/prometheus-serviceaccount.yaml |  kubectl create --namespace "$NS" -f -

kubectl create --namespace "$NS" -f $K8S/prometheus/prometheus-clusterroles.yaml
yq ".subjects[0].namespace=\"$NS\"" $K8S/prometheus-proxy/prometheus-proxy-clusterrolebinding.yaml | kubectl create --namespace "$NS" -f -
kubectl create --namespace "$NS" -f $K8S/prometheus-proxy/prometheus-proxy-deployment.yaml
kubectl create --namespace "$NS" -f $K8S/prometheus-proxy/prometheus-proxy-service.yaml
yq ".metadata.namespace=\"$NS\"" $K8S/prometheus-proxy/prometheus-proxy-serviceaccount.yaml |  kubectl create --namespace "$NS" -f -

kubectl create --namespace "$NS" -f $K8S/prometheus/prometheus-configmap.yaml
kubectl create --namespace "$NS" -f $K8S/prometheus/prometheus-deployment.yaml
kubectl create --namespace "$NS" -f $K8S/prometheus/prometheus-service.yaml
kubectl create --namespace "$NS" -f $K8S/grafana/

kubectl rollout status deployment --namespace "$NS" grafana
kubectl rollout status deployment --namespace "$NS" prometheus
kubectl rollout status deployment --namespace "$NS" prometheus-rsocket-proxy
GRAFANA_HOST=$(kubectl get --namespace $NS services grafana | grep -F grafana | awk '{ print $3 }')
echo "Set dashboard url=$GRAFANA_HOST:3000"
yq ".scdf.server.metrics.dashboard.url=\"http://$GRAFANA_HOST:3000\"" -i ./scdf-values.yml
yq ".scdf.feature.monitoring.grafana.enabled=true" -i ./scdf-values.yml
yq ".scdf.feature.monitoring.prometheusRsocketProxy.enabled=true" -i ./scdf-values.yml
end_time=$(date +%s)
elapsed=$((end_time - start_time))
echo -e "Deployed Prometheus, Prometheus proxy and Grafana in ${bold}$elapsed${end} seconds"

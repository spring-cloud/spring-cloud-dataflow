#!/usr/bin/env bash
EXT_IP=$(kubectl get services --namespace "$NS" scdf-server | grep -F "scdf" | grep -F "server" | awk '{ print $4 }')
LB_IP=$(kubectl get  --namespace "$NS" svc/scdf-server -o=jsonpath='{.status.loadBalancer.ingress[0].ip}')
PORT=$(kubectl get  --namespace "$NS" svc/scdf-server -o=jsonpath='{.spec.ports[0].port}')
if [ "$LB_IP" = "" ]; then
  LB_IP=$EXT_IP
fi
if [ "$PORT" = "" ]; then
  PORT="80"
fi
export DATAFLOW_URL="http://$LB_IP:$PORT"
echo "Dataflow URL: $DATAFLOW_URL"

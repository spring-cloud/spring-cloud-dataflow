#!/usr/bin/env bash
(return 0 2>/dev/null) && sourced=1 || sourced=0

if [ "$NS" = "" ]; then
  echo "NS not defined" >&2
  return 2
fi
if [ "$K8S_DRIVER" = "" ]; then
  K8S_DRIVER=kind
fi
EXTERNAL_IP=$(kubectl get --namespace "$NS" services scdf-server | grep -F "scdf-server" | awk '{ print $4 }')
LB_IP=$(kubectl get  --namespace "$NS" svc/scdf-server -o=jsonpath='{.status.loadBalancer.ingress[0].ip}')
echo "LB_IP=$LB_IP"
echo "EXTERNAL_IP=$EXTERNAL_IP"
if [ "$EXTERNAL_IP" = "<pending>" ]; then
  EXTERNAL_IP=$LB_IP
fi
export PLATFORM_TYPE=kubernetes
if [ "$EXTERNAL_IP" != "" ]; then
  export DATAFLOW_IP=http://$EXTERNAL_IP:9393
  export DATAFLOW_URL="$DATAFLOW_IP"
  echo "DATAFLOW_URL=$DATAFLOW_IP"
else
  echo "EXTERNAL_IP not found"
  kubectl get --namespace "$NS" services scdf-server
fi

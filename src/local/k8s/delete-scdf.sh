#!/bin/bash

kubectl delete deployments --all $WAIT --namespace="$NS"
kubectl delete statefulsets --all $WAIT --namespace="$NS"
kubectl delete svc --all $WAIT --namespace="$NS"
kubectl delete all --all $WAIT --namespace="$NS"
kubectl delete pods --all $WAIT --namespace="$NS"
kubectl delete secrets --all $WAIT --namespace="$NS"
kubectl delete configmap --all $WAIT --namespace="$NS"
kubectl delete pvc --all $WAIT --namespace="$NS"
kubectl delete secrets --namespace "$NS" --all
kubectl delete pvc --namespace "$NS" --all
if [ "$PROMETHEUS" = "true" ]; then
    echo "Removing ClusterRoles, ClusterRoleBindings, and ServiceAccounts for prometheus"
    kubectl delete clusterroles prometheus
    kubectl delete clusterrolebindings prometheus
    kubectl delete serviceaccounts prometheus --namespace "$NS"
    kubectl delete clusterroles prometheus-proxy
    kubectl delete clusterrolebindings prometheus-proxy
    kubectl delete serviceaccounts prometheus-proxy --namespace "$NS"
fi
if [ "$K8S_DRIVER" != "tmc" ] && [ "$K8S_DRIVER" != "gke" ] ; then
  echo "stopping port forward"
  kubectl_pid=$(ps aux | grep 'kubectl' | grep 'port\-forward' | awk '{print $2}')
  if [ "$kubectl_pid" != "" ]
  then
    kill $kubectl_pid
  fi
fi
if [ "$NS" != "default" ]; then
  kubectl delete namespace "$NS"
fi

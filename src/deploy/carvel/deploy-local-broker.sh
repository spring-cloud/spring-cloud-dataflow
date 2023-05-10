#!/usr/bin/env bash
function count_kind() {
    jq --arg kind $1 --arg name $2 '.items | .[] | select(.kind == $kind) | .metadata | select(.name == $name) | .name' | grep -c -F "$2"
}

SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
if [ "$BROKER" = "" ]; then
    echo "BROKER must be defined"
    exit 1
fi
start_time=$(date +%s)
case $BROKER in
"kafka")
    DEPLOYMENT_NAME=kafka-broker
    BINDER_NAME=kafka
    ;;
"rabbit" | "rabbitmq")
    BROKER=rabbitmq
    DEPLOYMENT_NAME=rabbitmq
    BINDER_NAME=rabbit
    ;;
*)
    echo "Invalid broker type $1"
    exit 1
    ;;
esac

K8S=$(realpath $SCDIR/../../kubernetes)
COUNT=$(kubectl get namespace $BROKER | grep -c "$BROKER")
if ((COUNT == 0)); then
    echo "Creating namespace $BROKER"
    kubectl create namespace "$BROKER"
else
    echo "Namespace $BROKER exists"
fi
kubectl create --namespace $BROKER -f $K8S/$BROKER/
if [ "$BROKER" = "rabbitmq" ]; then
    kubectl rollout status deployment --namespace "rabbitmq" rabbitmq
else
    kubectl rollout status deployment --namespace "kafka" kafka-zk
    kubectl rollout status sts --namespace "kafka" kafka-broker
fi
PRESENT=$(kubectl get serviceaccount --namespace $BROKER --output=json | count_kind serviceaccount "$BROKER-sa")
if ((PRESENT > 0)); then
  kubectl delete serviceaccount "$BROKER-sa" --namespace $BROKER
fi
kubectl create serviceaccount "$BROKER-sa" --namespace $BROKER

BROKER_HOST=$(kubectl get --namespace $BROKER services $DEPLOYMENT_NAME | grep -F $DEPLOYMENT_NAME | awk '{ print $3 }')
echo "Deployed $BROKER"
export BROKER
export BROKER_HOST

if [ "$BROKER" = "rabbitmq" ]; then
    yq ".scdf.binder.type=\"rabbit\"" -i ./scdf-values.yml
    yq ".scdf.binder.rabbit.host=\"${BROKER}.${BROKER}.svc.cluster.local\"" -i ./scdf-values.yml
    yq ".scdf.binder.rabbit.port=5672" -i ./scdf-values.yml
else
    yq ".scdf.binder.type=\"kafka\"" -i ./scdf-values.yml
    yq ".scdf.binder.kafka.broker.host=\"kafka-broker.$BROKER.svc.cluster.local:9092\"" -i ./scdf-values.yml
    yq ".scdf.binder.kafka.zk.host=\"kafka-zk.$BROKER.svc.cluster.local:2181\"" -i ./scdf-values.yml
fi
end_time=$(date +%s)
elapsed=$((end_time - start_time))
echo -e "Deployed $BROKER in ${bold}$elapsed${end} seconds"

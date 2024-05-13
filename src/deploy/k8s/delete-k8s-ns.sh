#!/usr/bin/env bash
WAIT=
CARVEL=false
if [ "$NS" = "" ]; then
    NS=scdf
fi
while [ "$1" != "" ]; do
    case $1 in
    "--nowait")
        WAIT="--wait=false"
        ;;
    "--wait")
        WAIT="--wait=true"
        ;;
    "--carvel")
        CARVEL=true
        ;;
    *)
        NS="$1"
        ;;
    esac
    shift
done
set +e
if [ "$VERBOSE" != "" ]; then
    kubectl get namespaces
fi
FOUND=$(kubectl get namespaces --output=json | jq --arg namespace $NS '.items | map(select(.metadata.name == $namespace)) | .[] | .metadata.name' | sed 's/\"//g')
if [ "$VERBOSE" != "" ]; then
    echo "FOUND=$FOUND"
fi
if [ "$FOUND" != "" ] && [ "$FOUND" != "null" ]; then
    echo "Deleting all resources in Namespace: $NS"
    if [ "$CARVEL" = "true" ]; then
        kubectl delete apps --all --wait=false --namespace="$NS"
        kubectl delete packageinstalls --all --wait=false --namespace="$NS"
        kubectl delete packagerepositories --all --wait=false --namespace="$NS"
    fi
    kubectl delete deployments --all $WAIT --namespace="$NS"
    kubectl delete statefulsets --all $WAIT --namespace="$NS"
    kubectl delete svc --all $WAIT --namespace="$NS"
    kubectl delete all --all $WAIT --namespace="$NS"
    kubectl delete pods --all $WAIT --namespace="$NS"
    kubectl delete secrets --all $WAIT --namespace="$NS"
    kubectl delete pvc --all $WAIT --namespace="$NS"
    if [ "$NS" != "default" ]; then
        set +e
        kubectl delete namespace "$NS" $WAIT --timeout=1m
        RC=$?
        set -e
        if ((RC == 0)); then
            echo "Deleted Namespace: $NS"
        else
            echo "Error $RC deleting Namespace: $NS"
        fi
    else
        echo "Cleaned Namespace: $NS"
    fi
else
    echo "Namespace:$NS doesn't exist"
fi

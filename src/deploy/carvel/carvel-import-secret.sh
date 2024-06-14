#!/usr/bin/env bash
if [ "$2" = "" ]; then
    echo "Argument required: <secret-name> <target-namespace>"
    exit 1
fi
IMPORT_TYPE=placeholder
SECRET_NAME=$1
NAMESPACE=$2
if [ "$3" != "" ] && [ "$3" != "--import" ] && [ "$3" != "--placeholder" ]; then
    FROM_NAMESPACE=$3
    shift
else
    FROM_NAMESPACE=secret-ns
fi
if [ "$3" = "--import" ]; then
    IMPORT_TYPE=import
elif [ "$3" = "--placeholder" ]; then
    IMPORT_TYPE=placeholder
fi
if [ "$SECRET_NAME" = "" ]; then
    echo "SECRET_NAME required"
    exit 2
fi
if [ "$NAMESPACE" = "" ]; then
    echo "NAMESPACE required"
    exit 2
fi
if [ "$IMPORT_TYPE" = "import" ]; then
kubectl apply -f -  <<EOF
apiVersion: secretgen.carvel.dev/v1alpha1
kind: SecretImport
metadata:
  name: $SECRET_NAME
  namespace: $NAMESPACE
spec:
  fromNamespace: $FROM_NAMESPACE
EOF
echo "Created SecretImport $SECRET_NAME from $FROM_NAMESPACE to $NAMESPACE"
else
kubectl apply -f -  <<EOF
apiVersion: v1
kind: Secret
metadata:
  name: $SECRET_NAME
  namespace: $NAMESPACE
  annotations:
    secretgen.carvel.dev/image-pull-secret: ""
type: kubernetes.io/dockerconfigjson
data:
  .dockerconfigjson: e30K
EOF
echo "Created Placeholder Secret $SECRET_NAME in $NAMESPACE"
fi

if [ "$DEBUG" = "true" ]; then
    kubectl describe secret $SECRET_NAME --namespace $NAMESPACE
fi
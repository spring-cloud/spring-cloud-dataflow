#!/usr/bin/env bash
if [ "$2" = "" ]; then
    echo "Argument required: <secret-name> <target-namespace>"
    exit 1
fi
SECRET_NAME=$1
NAMESPACE=$2
[ "$3" != "" ] && IMPORT_TYPE=import || IMPORT_TYPE=placeholder
[ "$3" != "" ] && FROM_NAMESPACE=$3 || FROM_NAMESPACE=secret-ns
if [ "$SECRET_NAME" = "" ]; then
    echo "SECRET_NAME required"
    exit 2
fi
if [ "$NAMESPACE" = "" ]; then
    echo "NAMESPACE required"
    exit 2
fi
if [ "$IMPORT_TYPE" == "import" ]; then
  echo "Creating SecretImport $SECRET_NAME from $FROM_NAMESPACE to $NAMESPACE"
  kubectl apply --namespace $NAMESPACE -f -  <<EOF
apiVersion: secretgen.carvel.dev/v1alpha1
kind: SecretImport
metadata:
  name: $SECRET_NAME
spec:
  fromNamespace: $FROM_NAMESPACE
EOF
  echo "Created SecretImport $SECRET_NAME from $FROM_NAMESPACE to $NAMESPACE"
else
  echo "Creating Placeholder Secret $SECRET_NAME in $NAMESPACE"
  kubectl apply --namespace $NAMESPACE -f - <<EOF
apiVersion: v1
kind: Secret
metadata:
  name: $SECRET_NAME
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

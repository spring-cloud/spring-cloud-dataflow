#!/usr/bin/env bash
if [ "$2" = "" ]; then
    echo "Argument required: <secret-name> <target-namespace>"
    exit 1
fi
SECRET_NAME=$1
NAMESPACE=$2
if [ "$3" != "" ]; then
    FROM_NAMESPACE=$3
else
    FROM_NAMESPACE=secret-ns
fi
if [ "$SECRET_NAME" = "" ]; then
    echo "SECRET_NAME required"
    exit 2
fi
if [ "$NAMESPACE" = "" ]; then
    echo "NAMESPACE required"
    exit 2
fi

FILE="$(mktemp).yml"
cat >$FILE <<EOF
apiVersion: secretgen.carvel.dev/v1alpha1
kind: SecretExport
metadata:
  name: $SECRET_NAME
  namespace: $FROM_NAMESPACE
spec:
  toNamespace: $NAMESPACE
EOF

echo "Create SecretExport $SECRET_NAME to $NS"
cat $FILE
kubectl apply -f $FILE
cat >$FILE <<EOF
apiVersion: secretgen.carvel.dev/v1alpha1
kind: SecretImport
metadata:
  name: $SECRET_NAME
  namespace: $NAMESPACE
spec:
  fromNamespace: $FROM_NAMESPACE
EOF
echo "Create SecretImport $SECRET_NAME from $FROM_NAMESPACE to $NAMESPACE"
cat $FILE
kubectl apply -f $FILE
rm -f $FILE

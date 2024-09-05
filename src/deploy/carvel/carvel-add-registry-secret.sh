#!/usr/bin/env bash
if [ -z "$BASH_VERSION" ]; then
    echo "This script requires Bash. Use: bash $0 $*"
    exit 1
fi
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
function check_env() {
  eval ev='$'$1
  if [ "$ev" == "" ]; then
    echo "env var $1 not defined"
    exit 1
  fi
}

function create_secret() {
    SCRT_NAME=$1
    REG_NAME=$2
    REG_USER=$3
    REG_PWD=$4
    SCRT_NS=$5
    echo "Create docker-registry secret $SCRT_NAME for $REG_NAME username=$REG_USER"
    kubectl create secret docker-registry "$SCRT_NAME" \
        --docker-server="$REG_NAME" \
        --docker-username="$REG_USER" \
        --docker-password="$REG_PWD" \
        --namespace "$SCRT_NS"
#    kubectl create secret docker-registry "$SCRT_NAME" \
#            --docker-server="$REG_NAME" \
#            --docker-username="$REG_USER" \
#            --docker-password="$4" \
#            --namespace "$NS"
    echo "Annotating $SCRT_NAME for image-pull-secret"
#    kubectl annotate secret "$1" --namespace "$NS"  secretgen.carvel.dev/image-pull-secret=""
    echo "Exporting $SCRT_NAME from $SCRT_NS"
    kubectl apply -f - <<EOF
apiVersion: secretgen.carvel.dev/v1alpha1
kind: SecretExport
metadata:
  name: ${SCRT_NAME}
  namespace: ${SCRT_NS}
spec:
  toNamespaces:
    - "*"
EOF

}
if [ "$4" = "" ]; then
    echo "Arguments: <secret-name> <registry-name> <registry-user> <registry-password>"
    exit 1
fi
SECRET_NAME=$1
REGISTRY_NAME=$2
REGISTRY_USER=$3
REGISTRY_PWD=$4
if [ "$5" != "" ]; then
    NS=$5
fi
check_env SECRET_NAME
check_env REGISTRY_NAME
check_env REGISTRY_USER
check_env NS
create_secret "$SECRET_NAME" "$REGISTRY_NAME" "$REGISTRY_USER" "$REGISTRY_PWD" "secrets-ns"

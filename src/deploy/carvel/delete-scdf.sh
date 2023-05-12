#!/usr/bin/env bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
function check_env() {
  eval ev='$'$1
  if [ "$ev" == "" ]; then
    echo "env var $1 not defined"
    if ((sourced != 0)); then
      return 1
    else
      exit 1
    fi
  fi
}
check_env NS

if [ "$SCDF_TYPE" == "" ]; then
    SCDF_TYPE=pro
fi
if [ "$1" != "" ]; then
  SCDF_TYPE=$1
fi

case $SCDF_TYPE in
"pro")
  APP_NAME=scdf-pro
  PACKAGE_VERSION=1.5.3-SNAPSHOT
  PACKAGE_NAME=scdfpro.tanzu.vmware.com
  REGISTRY=dev.registry.pivotal.io
  REPO_NAME="p-scdf-for-kubernetes/scdf-pro-repo"
  ;;
"oss")
  APP_NAME=scdf-oss
  PACKAGE_VERSION=2.11.0-SNAPSHOT
  PACKAGE_NAME=scdf.tanzu.vmware.com
  REGISTRY=index.docker.io
  REPO_NAME="springcloud/scdf-oss-repo"
  ;;
*)
  echo "Invalid SCDF_TYPE=$SCDF_TYPE only pro or oss is acceptable"
esac
echo "Deleting $APP_NAME from $NS"
kctrl package installed delete --package-install $APP_NAME --namespace $NS --yes
kctrl package repository delete --namespace $NS --repository $PACKAGE_NAME --yes

kubectl delete packagerepositories --all  --namespace="$NS"
kubectl delete packageinstalls --all --namespace="$NS"
kubectl delete apps --all --namespace="$NS"
kubectl delete deployments --all --namespace="$NS"
kubectl delete statefulsets --all --namespace="$NS"
kubectl delete svc --all --namespace="$NS"
kubectl delete all --all --namespace="$NS"
kubectl delete pods --all --namespace="$NS"
kubectl delete pvc --all --namespace="$NS"
kubectl delete configmaps --all --namespace="$NS"
kubectl delete secrets --all --namespace="$NS"
kubectl delete namespace $NS

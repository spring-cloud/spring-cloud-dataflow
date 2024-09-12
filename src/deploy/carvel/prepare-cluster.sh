#!/usr/bin/env bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
K8S=$(realpath $SCDIR/../k8s)
bold="\033[1m"
dim="\033[2m"
end="\033[0m"

readonly PULL_IMAGES="${PULL_IMAGES:false}"

if [ "$CERT_MANAGER_VERSION" = "" ]; then
  CERT_MANAGER_VERSION=v1.14.2
fi
if [ "$SECRETGEN_CONTROLLER_VERSION" = "" ]; then
  SECRETGEN_CONTROLLER_VERSION=v0.17.0
fi
if [ "$KAPP_CONTROLLER_VERSION" = "" ]; then
  KAPP_CONTROLLER_VERSION=v0.53.1
fi
if [ "$PULL_IMAGES" == "true" ]; then
  wget -q -O cert-manager-release.yml "https://github.com/cert-manager/cert-manager/releases/download/$CERT_MANAGER_VERSION/cert-manager.yaml"
  IMAGES=$(yq '.spec.template.spec.containers | .[] | .image' cert-manager-release.yml)
  for image in $IMAGES; do
    if [[ "$image" != "---"* ]]; then
      $K8S/load-image.sh "$image"
    fi
  done
  wget -q -O secretgen-release.yml "https://github.com/carvel-dev/secretgen-controller/releases/download/$SECRETGEN_CONTROLLER_VERSION/release.yml"
  IMAGES=$(yq '.spec.template.spec.containers | .[] | .image' secretgen-release.yml)
  for image in $IMAGES; do
    if [[ "$image" != "---"* ]]; then
      $K8S/load-image.sh "$image"
    fi
  done
  wget -q -O kapp-controller-release.yml https://github.com/carvel-dev/kapp-controller/releases/download/$KAPP_CONTROLLER_VERSION/release.yml
  IMAGES=$(yq '.spec.template.spec.containers | .[] | .image' kapp-controller-release.yml)
  for image in $IMAGES; do
    if [[ "$image" != "---"* ]]; then
      $K8S/load-image.sh "$image"
    fi
  done
fi
start_time=$(date +%s)
echo "Deploying cert-manager $CERT_MANAGER_VERSION"
set -e
kapp deploy --yes --wait --wait-check-interval 10s --app cert-manager \
    --file https://github.com/cert-manager/cert-manager/releases/download/$CERT_MANAGER_VERSION/cert-manager.yaml
echo "Deployed cert-manager $CERT_MANAGER_VERSION"
echo "Deploying secretgen-controller $SECRETGEN_CONTROLLER_VERSION"
kapp deploy --yes --wait --wait-check-interval 10s --app secretgen-controller \
    --file https://github.com/carvel-dev/secretgen-controller/releases/download/$SECRETGEN_CONTROLLER_VERSION/release.yml
echo "Deployed secretgen-controller"
echo "Deploying kapp-controller $KAPP_CONTROLLER_VERSION"
kapp deploy --yes --wait --wait-check-interval 10s --app kapp-controller --file https://github.com/carvel-dev/kapp-controller/releases/download/$KAPP_CONTROLLER_VERSION/release.yml
echo "Deployed kapp-controller"
end_time=$(date +%s)
elapsed=$((end_time - start_time))
echo -e "Prepared cluster in ${bold}$elapsed${end} seconds"

#!/usr/bin/env bash
bold="\033[1m"
dim="\033[2m"
end="\033[0m"
if [ "$CERT_MANAGER_VERSION" = "" ]; then
  CERT_MANAGER_VERSION=v1.14.2
fi
if [ "$SECRETGEN_CONTROLLER_VERSION" = "" ]; then
  SECRETGEN_CONTROLLER_VERSION=v0.17.0
fi
if [ "$KAPP_CONTROLLER_VERSION" = "" ]; then
  KAPP_CONTROLLER_VERSION=v0.53.0
fi
start_time=$(date +%s)
echo "Deploying cert-manager $CERT_MANAGER_VERSION"
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

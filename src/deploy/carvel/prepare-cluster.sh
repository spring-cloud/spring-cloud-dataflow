#!/usr/bin/env bash
CM_VERSION=v1.11.1
SGC_VER=v0.14.2
KC_VER=v0.45.0
start_time=$(date +%s)
echo "Deploying cert-manager $CM_VERSION"
kapp deploy --yes --wait --wait-check-interval 10s --app cert-manager \
    --file https://github.com/cert-manager/cert-manager/releases/download/$CM_VERSION/cert-manager.yaml
echo "Deployed cert-manager $CM_VERSION"
echo "Deploying secretgen-controller $SGC_VER"
kapp deploy --yes --wait --wait-check-interval 10s --app secretgen-controller \
    --file https://github.com/carvel-dev/secretgen-controller/releases/download/$SGC_VER/release.yml
echo "Deployed secretgen-controller"
echo "Deploying kapp-controller $KC_VER"
kapp deploy --yes --wait --wait-check-interval 10s --app kapp-controller --file https://github.com/carvel-dev/kapp-controller/releases/download/$KC_VER/release.yml
echo "Deployed kapp-controller"
end_time=$(date +%s)
elapsed=$((end_time - start_time))
echo -e "Prepared cluster in ${bold}$elapsed${end} seconds"

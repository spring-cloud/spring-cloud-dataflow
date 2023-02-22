#!/usr/bin/env bash
echo "Deploying cert-manager"
kapp deploy --yes --wait --wait-check-interval 10s --app cert-manager \
    --file https://github.com/cert-manager/cert-manager/releases/latest/download/cert-manager.yaml
echo "Deployed cert-manager"
echo "Deploying secretgen-controller"
kapp deploy --yes --wait --wait-check-interval 10s --app secretgen-controller \
    --file https://github.com/carvel-dev/secretgen-controller/releases/latest/download/release.yml
echo "Deployed secretgen-controller"
echo "Deploying kapp-controller"
kapp deploy --yes --wait --wait-check-interval 10s --app kapp-controller --file https://github.com/carvel-dev/kapp-controller/releases/latest/download/release.yml
echo "Deployed kapp-controller"
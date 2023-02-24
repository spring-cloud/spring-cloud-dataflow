#!/usr/bin/env bash
echo "Deploying cert-manager"
kapp deploy --yes --wait --wait-check-interval 10s --app cert-manager \
    --file manifests-download/cert-manager.yaml
echo "Deployed cert-manager"
echo "Deploying secretgen-controller"
kapp deploy --yes --wait --wait-check-interval 10s --app secretgen-controller \
    --file manifests-download/secretgen-controller.yaml
echo "Deployed secretgen-controller"
echo "Deploying kapp-controller"
kapp deploy --yes --wait --wait-check-interval 10s --app kapp-controller --file manifests-download/kapp-controller.yaml
echo "Deployed kapp-controller"
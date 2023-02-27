#   export DOCKER_HUB_USERNAME="<docker-hub-username>"
#   export DOCKER_HUB_PASSWORD="<docker-hub-password>"

export INTERNAL_REGISTRY="sehsahdemo.azurecr.io"

#################################### cert-manager ######################################################

export CM_1="quay.io/jetstack/cert-manager-cainjector:v1.11.0"
export CM_2="quay.io/jetstack/cert-manager-controller:v1.11.0"
export CM_3="quay.io/jetstack/cert-manager-webhook:v1.11.0"
export CM_4="quay.io/jetstack/cert-manager-acmesolver:v1.11.0"


export CM_1_INTERNAL="${INTERNAL_REGISTRY}/scdf/cert-manager/cert-manager-cainjector:v1.11.0"
export CM_2_INTERNAL="${INTERNAL_REGISTRY}/scdf/cert-manager/cert-manager-controller:v1.11.0"
export CM_3_INTERNAL="${INTERNAL_REGISTRY}/scdf/cert-manager/cert-manager-webhook:v1.11.0"
export CM_4_INTERNAL="${INTERNAL_REGISTRY}/scdf/cert-manager/cert-manager-acmesolver:v1.11.0"

docker pull $CM_1 --platform linux/amd64
docker tag $CM_1 $CM_1_INTERNAL
docker push $CM_1_INTERNAL
docker pull $CM_2 --platform linux/amd64
docker tag $CM_2 $CM_2_INTERNAL
docker push $CM_2_INTERNAL
docker pull $CM_3 --platform linux/amd64
docker tag $CM_3 $CM_3_INTERNAL
docker push $CM_3_INTERNAL
docker pull $CM_4 --platform linux/amd64
docker tag $CM_4 $CM_4_INTERNAL
docker push $CM_4_INTERNAL


sed -i -e "s~$CM_1~$CM_1_INTERNAL~g" ./manifests-download/cert-manager.yaml
sed -i -e "s~$CM_2~$CM_2_INTERNAL~g" ./manifests-download/cert-manager.yaml
sed -i -e "s~$CM_3~$CM_3_INTERNAL~g" ./manifests-download/cert-manager.yaml
sed -i -e "s~$CM_4~$CM_4_INTERNAL~g" ./manifests-download/cert-manager.yaml

#################################### secretgen-controller ######################################################

export SC_1="ghcr.io/carvel-dev/secretgen-controller@sha256:6a0e2ce63f5ebae239f230a2e1b8d9c717e26fd78f54dbe5d07012d4ad2bb340"

export SC_1_INTERNAL="${INTERNAL_REGISTRY}/scdf/carvel-dev/secretgen-controller"

docker pull $SC_1 --platform linux/amd64
docker tag $SC_1 $SC_1_INTERNAL
docker push $SC_1_INTERNAL

SC_1_INTERNAL_SHA=$(docker inspect --format='{{index .RepoDigests 1}}' $SC_1_INTERNAL)

sed -i -e "s~$SC_1~$SC_1_INTERNAL_SHA~g" ./manifests-download/secretgen-controller.yaml


#################################### kapp-controller ######################################################


export KAP_1="ghcr.io/carvel-dev/kapp-controller@sha256:eea5193161109c3685edad596b4b740a0683a0cb892f2a9cf94af96c6008203b"

export KAP_1_INTERNAL="${INTERNAL_REGISTRY}/scdf/carvel-dev/kapp-controller"

docker pull $KAP_1 --platform linux/amd64
docker tag $KAP_1 $KAP_1_INTERNAL
docker push $KAP_1_INTERNAL

KAP_1_INTERNAL_SHA=$(docker inspect --format='{{index .RepoDigests 1}}' $KAP_1_INTERNAL)

sed -i -e "s~$KAP_1~$KAP_1_INTERNAL_SHA~g" ./manifests-download/kapp-controller.yaml
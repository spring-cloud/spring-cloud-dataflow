#!/usr/bin/env bash
set -e
IMG_PKG_OPT=
if [ "$USE_SRP" == "true" ]; then
    IMG_PKG_OPT="--debug"
    if [ "$SSL_CERT_FILE" != "" ] && [ -f "$SSL_CERT_FILE" ]; then
        IMG_PKG_OPT="$IMG_PKG_OPT --registry-ca-cert-path $SSL_CERT_FILE"
    else
        IMG_PKG_OPT="$IMG_PKG_OPT --registry-verify-certs=false"
    fi
fi
if [ "$IMG_PKG_OPT" != "" ]; then
    echo "IMG_PKG_OPT=$IMG_PKG_OPT"
fi
set +e
IMAGE_ID=$(docker images --digests --format json | jq -r IMAGE_URL "${REPOSITORY}" --arg TAG "${VERSION}" 'select(.Repository == $IMAGE_URL and .Tag == $TAG)' | jq -r --slurp 'map({ID: .ID}) | unique | .[] | .ID')
if [ "$IMAGE_ID" != "" ]; then
    docker rmi --force "$REPOSITORY:$VERSION"
fi
set -e
imgpkg push $IMG_PKG_OPT --bundle "$REPOSITORY:$VERSION-RANDOM.$RTAG" --file "$BUNDLE_PATH"
docker pull "$REPOSITORY:$VERSION-RANDOM.$RTAG"
docker tag "$REPOSITORY:$VERSION-RANDOM.$RTAG" "$REPOSITORY:$VERSION"
docker push "$REPOSITORY:$VERSION"


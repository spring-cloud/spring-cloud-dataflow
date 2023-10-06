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
docker rmi "$REPOSITORY:$VERSION" > /dev/null
set -e
imgpkg push $IMG_PKG_OPT --bundle "$REPOSITORY:$VERSION-RANDOM.$RTAG" --file "$BUNDLE_PATH"
docker pull "$REPOSITORY:$VERSION-RANDOM.$RTAG"
docker tag "$REPOSITORY:$VERSION-RANDOM.$RTAG" "$REPOSITORY:$VERSION"
docker push "$REPOSITORY:$VERSION"


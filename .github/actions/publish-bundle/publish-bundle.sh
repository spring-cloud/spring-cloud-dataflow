#!/usr/bin/env bash
set -e
IMG_PKG_OPT=
if [ "$USE_SRP" == "true" ]; then
    IMG_PKG_OPT="--debug"
    if [ "$NODE_EXTRA_CA_CERTS" != "" ]; then
        IMG_PKG_OPT="$IMG_PKG_OPT --registry-ca-cert-path $NODE_EXTRA_CA_CERTS"
    else
        IMG_PKG_OPT="$IMG_PKG_OPT --registry-verify-certs=false"
    fi
fi
if [ "$IMG_PKG_OPT" != "" ]; then
    echo "IMG_PKG_OPT=$IMG_PKG_OPT"
fi
imgpkg push $IMG_PKG_OPT --bundle "$REPOSITORY:$VERSION-RANDOM.$RTAG" --file "$BUNDLE_PATH"
docker pull "$REPOSITORY:$VERSION-RANDOM.$RTAG"
docker tag "$REPOSITORY:$VERSION-RANDOM.$RTAG $REPOSITORY:$VERSION"
docker push "$REPOSITORY:$VERSION"


#!/usr/bin/env bash
set -e
IMG_PKG_OPT=
if [ "$USE_SRP" == "true" ]; then
    IMG_PKG_OPT=--debug
fi
imgpkg push $IMG_PKG_OPT --bundle $REPOSITORY:$VERSION-RANDOM.$RTAG --file $BUNDLE_PATH
docker pull $REPOSITORY:$VERSION-RANDOM.$RTAG
docker tag $REPOSITORY:$VERSION-RANDOM.$RTAG $REPOSITORY:$VERSION
docker push $REPOSITORY:$VERSION

#!/usr/bin/env bash
set -e
IMG_PKG_OPT=
if [ "$USE_SRP" == "true" ]; then
    IMG_PKG_OPT="--debug --registry-verify-certs"
# TODO This is for diagnostic purposes.  It will be removed
    ls -al /tmp/srp-tools/observer/bin/runtime/logs/

fi
# --registry-ca-cert-path strings
#
imgpkg push $IMG_PKG_OPT --bundle $REPOSITORY:$VERSION-RANDOM.$RTAG --file $BUNDLE_PATH
docker pull $REPOSITORY:$VERSION-RANDOM.$RTAG
docker tag $REPOSITORY:$VERSION-RANDOM.$RTAG $REPOSITORY:$VERSION
docker push $REPOSITORY:$VERSION

# TODO This is for diagnostic purposes.  It will be removed.
if [ "$USE_SRP" == "true" ]; then
    echo "=================>>>>>>>>observer logs<<<<<<<<<<=============="
    ls -al /tmp/srp-tools/observer/bin/runtime/logs/
    cat /tmp/srp-tools/observer/bin/runtime/logs/access.log
    cat /tmp/srp-tools/observer/bin/runtime/logs/mitmdump_error.log
    echo "=================>>>>>>>>end observer logs<<<<<<<<<<=========="
fi

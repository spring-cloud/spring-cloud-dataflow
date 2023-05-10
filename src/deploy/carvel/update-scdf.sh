#!/usr/bin/env bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
start_time=$(date +%s)
# the following names are your choice.
if [ "$NS" = "" ]; then
    echo "Expected env var NS"
    exit 1
fi
SA=$NS-sa
if [ "$SCDF_TYPE" == "" ]; then
    SCDF_TYPE=pro
fi
if [ "$1" != "" ]; then
    SCDF_TYPE=$1
fi

case $SCDF_TYPE in
"pro")
    APP_NAME=scdf-pro
    PACKAGE_VERSION=1.5.3-SNAPSHOT
    ;;
"oss")
    APP_NAME=scdf-oss
    PACKAGE_VERSION=2.11.0-SNAPSHOT
    ;;
*)
    echo "Invalid SCDF_TYPE=$SCDF_TYPE only pro or oss is acceptable"
    ;;
esac
echo "Updating SCDF-$SCDF_TYPE $PACKAGE_VERSION as $APP_NAME"
set +e
kctrl package installed update --package-install $APP_NAME \
    --values-file "./scdf-values.yml" \
    --version $PACKAGE_VERSION --namespace "$NS" --yes \
    --wait --wait-check-interval 10s

kctrl app status --app $APP_NAME --namespace $NS --json
kctrl package installed status --package-install $APP_NAME --namespace $NS
end_time=$(date +%s)
elapsed=$((end_time - start_time))
echo -e "Updated SCDF Package in ${bold}$elapsed${end} seconds"

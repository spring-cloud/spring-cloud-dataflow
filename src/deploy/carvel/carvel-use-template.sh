#!/usr/bin/env bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")

if [ "$SCDF_TYPE" = "" ]; then
    echo "SCDF_TYPE must be configured"
    exit 1
fi
echo "Copying scdf-$SCDF_TYPE-values.yml to ./scdf-values.yml"
cp "$SCDIR/scdf-${SCDF_TYPE}-values.yml" ./scdf-values.yml

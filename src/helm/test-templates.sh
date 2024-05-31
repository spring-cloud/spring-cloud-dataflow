#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
pushd "$SCDIR" > /dev/null || exit
helm template --debug scdf/ --values scdf/test-values.yaml | tee test.yml
helm lint --debug scdf/ --values scdf/test-values.yaml
popd > /dev/null || exit

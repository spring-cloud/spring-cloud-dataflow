#!/bin/bash
set -e
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
rm -f rewrite.log
REST="$@"
find . -name pom.xml -type f -exec "$SCDIR/apply-rewrite.sh" '{}' $REST \; | tee -a rewrite.log

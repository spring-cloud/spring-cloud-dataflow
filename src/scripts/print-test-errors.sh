#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
find . -type d -name surefire-reports -exec $SCDIR/find-test-errors.sh {} \;

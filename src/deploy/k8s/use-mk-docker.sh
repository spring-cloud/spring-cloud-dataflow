#!/usr/bin/env bash
if [ -z "$BASH_VERSION" ]; then
    echo "This script requires Bash. For example:\n bash \n $0 $*"
else
    echo "Setting variables"
    SCDIR=$(readlink -f "${BASH_SOURCE[0]}")
    SCDIR=$(dirname "$SCDIR")
    SCDIR=$(realpath $SCDIR)
    source $SCDIR/use-mk.sh docker $*
fi

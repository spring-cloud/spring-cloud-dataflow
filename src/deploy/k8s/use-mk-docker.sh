#!/usr/bin/env bash
SCDIR=$(readlink -f "${BASH_SOURCE[0]}")
SCDIR=$(dirname "$SCDIR")
SCDIR=$(realpath $SCDIR)
source $SCDIR/use-mk.sh docker $*

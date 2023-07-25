#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)
source $SCDIR/use-mk.sh kvm2 $*

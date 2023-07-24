#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
$SCDIR/delete-k8s-ns.sh $NS


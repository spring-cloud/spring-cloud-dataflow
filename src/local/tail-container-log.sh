#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)
ID=$($SCDIR/container-id.sh $1)
docker logs -f -n all $ID

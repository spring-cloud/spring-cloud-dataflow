#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
rm -f rewrite.log
if [ "$1" = "" ]; then
    echo "Usage $0 <command> [recipes]"
    exit 1
fi
CMD=$1
shift
RECIPES="1 2 3"
if [ "$1" != "" ]; then
    RECIPES=
fi
while [ "$1" != "" ]; do
    RECIPES="$RECIPES $1"
    shift
done
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
find . -depth -name pom.xml -exec $SCDIR/apply-rewrite.sh '{}' $CMD $RECIPES \;

#!/usr/bin/env bash
PARENT=$(realpath $(dirname "$2" ))
set +e
xsltproc --load-trace --stringparam file $(basename "$PARENT") "$1" "$2"
RC=$?
if [ "$RC" != "0" ]; then
  exit $RC
fi
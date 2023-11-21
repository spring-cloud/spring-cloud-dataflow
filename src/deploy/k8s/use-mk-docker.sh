#!/usr/bin/env bash
(return 0 2>/dev/null) && sourced=1 || sourced=0
if [ "$sourced" = "0" ]; then
  echo "This script must be invoked using: source $0 $*"
  exit 1
fi
if [ -z "$BASH_VERSION" ]; then
    echo "This script requires Bash. Use: bash $0 $*"
    exit 0
fi
SCDIR=$(readlink -f "${BASH_SOURCE[0]}")
SCDIR=$(dirname "$SCDIR")
SCDIR=$(realpath $SCDIR)
source $SCDIR/use-mk.sh docker $*

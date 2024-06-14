#!/usr/bin/env bash
if [ -n "$BASH_SOURCE" ]; then
  SCDIR="$(readlink -f "${BASH_SOURCE[0]}")"
elif [ -n "$ZSH_VERSION" ]; then
  setopt function_argzero
  SCDIR="${(%):-%N}"
elif eval '[[ -n ${.sh.file} ]]' 2>/dev/null; then
  eval 'SCDIR=${.sh.file}'
else
  echo 1>&2 "Unsupported shell. Please use bash, ksh93 or zsh."
  exit 2
fi
SCDIR="$(dirname "$SCDIR")"


if [ "$SCDF_TYPE" = "" ]; then
    echo "SCDF_TYPE must be configured"
    exit 1
fi
echo "Copying scdf-$SCDF_TYPE-values.yml to ./scdf-values.yml"
cp "$SCDIR/scdf-${SCDF_TYPE}-values.yml" ./scdf-values.yml

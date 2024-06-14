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

function check_env() {
  eval ev='$'$1
  if [ "$ev" = "" ]; then
    echo "env var $1 not defined"
    exit 1
  fi
}


if [ "$4" = "" ]; then
    echo "Arguments: <secret-name> <registry-name> <registry-user> <registry-password>"
    exit 1
fi
SECRET_NAME=$1
REGISTRY_NAME=$2
REGISTRY_USER=$3
REGISTRY_PWD=$4
SECRET_NS=$NS
if [ "$5" != "" ]; then
    SECRET_NS=$5
fi
check_env SECRET_NAME
check_env SECRET_NS

#kubectl create secret docker-registry "$SECRET_NAME" \
#    --docker-server="$REGISTRY_NAME" \
#    --docker-username="$REGISTRY_USER" \
#    --docker-password="$REGISTRY_PWD" \
#    --namespace "$NS"

"$SCDIR/carvel-import-secret.sh" "$SECRET_NAME" "$SECRET_NS"

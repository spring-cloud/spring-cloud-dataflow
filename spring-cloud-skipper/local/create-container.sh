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

ROOT_DIR=$(realpath "$SCDIR/..")
# set to specific version
if [ "$1" != "" ]; then
    TAG=$1
else
    TAG=2.9.0-SNAPSHOT
fi
if [ "$2" != "" ]; then
    v=$2
else
    v=11
fi
PROCESSOR=$(uname -p)
if [ "$ARCH" = "" ]; then
    case $PROCESSOR in
    "x86_64")
        ARCH=amd64
        ;;
    *)
        if [[ "$PROCESSOR" = *"arm"* ]]; then
            ARCH=arm64v8
        fi
        ;;
    esac
fi
IMAGE="$ARCH/eclipse-temurin:$v-jdk-jammy"

CRED=
if [ "$DOCKER_USERNAME" != "" ]; then
    CRED="--from-username=$DOCKER_USERNAME --from-password=$DOCKER_PASSWORD"
fi
jib jar --from=$IMAGE $CRED \
    --target=docker://springcloud/spring-cloud-skipper-server:$TAG \
    "$ROOT_DIR/spring-cloud-skipper-server/target/spring-cloud-skipper-server-$TAG.jar"

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

ROOT="$(realpath "$SCDIR/../..")"
pushd "$ROOT" > /dev/null || exit
    ./mvnw install -DskipTests -am -pl :spring-cloud-dataflow-classic-docs,:spring-cloud-dataflow-docs,:spring-cloud-skipper-server-core,:spring-cloud-skipper-docs -Pfull,asciidoctordocs,restdocs
popd > /dev/null || exit

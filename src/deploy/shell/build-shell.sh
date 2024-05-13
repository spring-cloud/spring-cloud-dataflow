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
SCDIR="$(realpath "$SCDIR")"
pushd "$SCDIR" > /dev/null
./mvnw -o -am -pl :spring-cloud-dataflow-shell install -DskipTests -T 0.5C
DATAFLOW_VERSION=$(./mvnw exec:exec -Dexec.executable='echo' -Dexec.args='${project.version}' --non-recursive -q | sed 's/\"//g' | sed 's/version=//g')
SRC="./spring-cloud-dataflow-shell/target/spring-cloud-dataflow-shell-${DATAFLOW_VERSION}.jar"
if [ ! -f "$SRC" ]; then
    echo "Cannot find $SRC"
fi
echo "Built $SRC"
cp "$SRC" ./src/deploy/shell/
echo "Copied: $SRC"
echo "set DATAFLOW_VERSION=$DATAFLOW_VERSION to use this version of the shell"
popd
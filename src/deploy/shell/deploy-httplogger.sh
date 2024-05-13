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

cat > $SCDIR/deploy-httplogger.shell <<EOF
stream create --name httplogger --definition "http | log"
stream deploy --name httplogger --properties app.*.logging.level.root=debug,deployer.http.kubernetes.createLoadBalancer=true
EOF

"$SCDIR/shell.sh" --spring.shell.commandFile=$SCDIR/deploy-httplogger.shell

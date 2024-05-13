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

cat > $SCDIR/deploy-ctr3b.shell <<EOF
task create --name timestamp-ctr3b --definition "timestamp-app-1: timestamp && timestamp-batch-1: timestamp-batch && timestamp-app-2: timestamp3 && timestamp-batch-2: timestamp-batch3"
task launch --name timestamp-ctr3b --properties app.composed-task-runner.logging.level.root=debug
EOF

"$SCDIR/shell.sh" --spring.shell.commandFile=$SCDIR/deploy-ctr3b.shell

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

cat > $SCDIR/register-apps.shell <<EOF
app register --uri docker:springcloudtask/timestamp-task:2.0.2 --name timestamp --type task --force
app register --uri docker:springcloudtask/timestamp-batch-task:2.0.2 --name timestamp-batch --type task --force
app register --uri docker:springcloudtask/timestamp-task:3.0.0 --name timestamp3 --bootVersion 3 --type task --force
app register --uri docker:springcloudtask/timestamp-batch-task:3.0.0 --name timestamp-batch3 --bootVersion 3 --type task --force
app register --uri docker:springcloudtask/task-demo-metrics-prometheus:2.0.1-SNAPSHOT --name task-demo-metrics-prometheus --type task --force
app register --uri docker:springcloudstream/time-source-rabbit:4.0.0-RC2 --name time --bootVersion 3 --type source --force
app register --uri docker:springcloudstream/log-sink-rabbit:4.0.0-RC2 --name log --bootVersion 3 --type sink --force
EOF

"$SCDIR/shell.sh" --spring.shell.commandFile=$SCDIR/register-apps.shell

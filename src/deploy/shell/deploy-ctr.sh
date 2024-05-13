#!/usr/bin/env bash
SCDIR="$(realpath $(dirname $(readlink -f "$0")))"

cat > $SCDIR/deploy-ctr.shell <<EOF
task create --name timestamp-ctr --definition "timestamp-app-1: timestamp && timestamp-batch-1: timestamp-batch && timestamp-app-2: timestamp && timestamp-batch-2: timestamp-batch"
task launch --name timestamp-ctr --properties app.composed-task-runner.logging.level.root=debug
EOF

"$SCDIR/shell.sh" --spring.shell.commandFile=$SCDIR/deploy-ctr.shell

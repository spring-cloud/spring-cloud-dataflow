#!/usr/bin/env bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")

cat > timestamp-ctr.shell <<EOF
task create --name timestamp-ctr --definition "timestamp-app1: timestamp && timestamp-batch-1: timestamp-batch && timestamp-app-2: timestamp && timestamp-batch-2: timestamp-batch"
task launch --name timestamp-ctr --properties app.composed-task-runner.logging.level.root=debug,app.composed-task-runner.spring.cloud.task.closecontext-enabled=true
EOF

$SCDIR/shell.sh --spring.shell.commandFile=timestamp-ctr.shell

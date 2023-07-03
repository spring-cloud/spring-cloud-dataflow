#!/usr/bin/env bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")

cat > ts-task.shell <<EOF
task create --name ts-task --definition "timestamp"
task launch --name ts-task
EOF

$SCDIR/shell.sh --spring.shell.commandFile=ts-task.shell

#!/usr/bin/env bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")

cat > ts-task3.shell <<EOF
task create --name ts-task3 --definition "timestamp3"
task launch --name ts-task3
EOF

$SCDIR/shell.sh --spring.shell.commandFile=ts-task3.shell

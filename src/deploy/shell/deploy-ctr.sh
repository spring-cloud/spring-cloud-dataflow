#!/usr/bin/env bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")

cat > timestamp-ctr.shell <<EOF
task create --name timestamp-ctr --definition "timestamp-app-1: timestamp-batch && timestamp-app-2: timestamp-batch"
task launch --name timestamp-ctr
EOF

$SCDIR/shell.sh --spring.shell.commandFile=timestamp-ctr.shell

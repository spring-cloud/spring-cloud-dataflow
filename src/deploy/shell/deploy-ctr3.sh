#!/usr/bin/env bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")

cat > timestamp-ctr3.shell <<EOF
task create --name timestamp-ctr3 --definition "timestamp-app-1: timestamp-batch && timestamp-app-3: timestamp-batch3"
task launch --name timestamp-ctr3
EOF

$SCDIR/shell.sh --spring.shell.commandFile=timestamp-ctr3.shell

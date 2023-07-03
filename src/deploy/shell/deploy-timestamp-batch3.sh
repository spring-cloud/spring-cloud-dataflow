#!/usr/bin/env bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")

cat > ts-batch3.shell <<EOF

task create --name ts-batch3 --definition "timestamp-batch3"
task launch --name ts-batch3
EOF

$SCDIR/shell.sh --spring.shell.commandFile=ts-batch3.shell

#!/usr/bin/env bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")

cat > ts-batch.shell <<EOF
task create --name ts-batch --definition "timestamp-batch"
task launch --name ts-batch
EOF

$SCDIR/shell.sh --spring.shell.commandFile=ts-batch.shell

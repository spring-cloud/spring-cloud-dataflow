#!/usr/bin/env bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")

cat > deploy-ts-batch.shell <<EOF
task create --name ts-batch --definition "timestamp-batch"
task launch --name ts-batch --properties app.*.logging.level.root=debug
EOF

"$SCDIR/shell.sh" --spring.shell.commandFile=deploy-ts-batch.shell

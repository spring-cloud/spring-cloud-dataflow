#!/usr/bin/env bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")

cat > deploy-ts-task.shell <<EOF
task create --name ts-task --definition "timestamp"
task launch --name ts-task --properties app.*.logging.level.root=debug
EOF
"$SCDIR/shell.sh" --spring.shell.commandFile=deploy-ts-task.shell

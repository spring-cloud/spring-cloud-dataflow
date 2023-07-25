#!/usr/bin/env bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")

cat > run-ts-task.shell <<EOF
task launch --name ts-task --properties app.*.logging.level.root=debug
EOF
"$SCDIR/shell.sh" --spring.shell.commandFile=run-ts-task.shell

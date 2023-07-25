#!/usr/bin/env bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")

cat > run-ts-task3.shell <<EOF
task launch --name ts-task3 --properties app.*.logging.level.root=debug
EOF

"$SCDIR/shell.sh" --spring.shell.commandFile=run-ts-task3.shell

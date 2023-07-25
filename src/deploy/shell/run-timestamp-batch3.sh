#!/usr/bin/env bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")

cat > run-ts-batch3.shell <<EOF
task launch --name ts-batch3 --properties app.*.logging.level.root=debug
EOF

"$SCDIR/shell.sh" --spring.shell.commandFile=run-ts-batch3.shell

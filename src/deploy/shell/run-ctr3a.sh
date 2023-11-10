#!/usr/bin/env bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")

cat > run-ctr3a.shell <<EOF
task launch --name timestamp-ctr3a --properties app.composed-task-runner.logging.level.root=debug
EOF
"$SCDIR/shell.sh" --spring.shell.commandFile=run-ctr3a.shell

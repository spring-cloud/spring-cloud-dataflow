#!/usr/bin/env bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")

cat > deploy-ctr3a.shell <<EOF
task create --name timestamp-ctr3a --definition "timestamp && timestamp-batch && timestamp3 && timestamp-batch3"
task launch --name timestamp-ctr3a --properties app.composed-task-runner.logging.level.root=debug
EOF

"$SCDIR/shell.sh" --spring.shell.commandFile=deploy-ctr3a.shell

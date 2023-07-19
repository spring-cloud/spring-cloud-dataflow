#!/usr/bin/env bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")

cat > run-task-demo-metrics-prometheus.shell <<EOF
task launch --name task-demo-metrics-prometheus --properties app.*.logging.level.root=debug
EOF

"$SCDIR/shell.sh" --spring.shell.commandFile=run-task-demo-metrics-prometheus.shell

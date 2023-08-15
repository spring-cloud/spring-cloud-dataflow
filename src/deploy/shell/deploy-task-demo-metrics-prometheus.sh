#!/usr/bin/env bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")

cat > $SCDIR/deploy-task-demo-metrics-prometheus.shell <<EOF
task create --name task-demo-metrics-prometheus --definition "task-demo-metrics-prometheus"
task launch --name task-demo-metrics-prometheus --properties app.*.logging.level.root=debug
EOF
"$SCDIR/shell.sh" --spring.shell.commandFile=$SCDIR/deploy-task-demo-metrics-prometheus.shell

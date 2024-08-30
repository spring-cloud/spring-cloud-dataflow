#!/usr/bin/env bash
if [ -z "$BASH_VERSION" ]; then
    echo "This script requires Bash. Use: bash $0 $*"
    exit 0
fi
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")

if [ "$BROKER" == "rabbitmq" ]; then
    BROKER_NAME=rabbit
else
    BROKER_NAME=$BROKER
fi

cat > $SCDIR/register-apps.shell <<EOF
app register --uri docker:springcloudtask/timestamp-task:2.0.2 --name timestamp --type task --force
app register --uri docker:springcloudtask/timestamp-batch-task:2.0.2 --name timestamp-batch --type task --force
app register --uri docker:springcloudtask/timestamp-task:3.0.0 --name timestamp3 --bootVersion 3 --type task --force
app register --uri docker:springcloudtask/timestamp-batch-task:3.0.0 --name timestamp-batch3 --bootVersion 3 --type task --force
app register --uri docker:springcloudtask/task-demo-metrics-prometheus:2.0.1-SNAPSHOT --name task-demo-metrics-prometheus --type task --force
app register --uri docker:springcloudstream/time-source-${BROKER_NAME}:5.0.0 --name time --bootVersion 3 --type source --force
app register --uri docker:springcloudstream/log-sink-${BROKER_NAME}:5.0.0 --name log --bootVersion 3 --type sink --force
EOF

"$SCDIR/shell.sh" --spring.shell.commandFile=$SCDIR/register-apps.shell

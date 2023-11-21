#!/usr/bin/env bash
if [ -z "$BASH_VERSION" ]; then
    echo "This script requires Bash. Use: bash $0 $*"
    exit 0
fi
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")

cat > $SCDIR/deploy-ctr3a.shell <<EOF
task create --name timestamp-ctr3a --definition "timestamp && timestamp-batch && timestamp3 && timestamp-batch3"
task launch --name timestamp-ctr3a --properties app.composed-task-runner.logging.level.root=debug
EOF

"$SCDIR/shell.sh" --spring.shell.commandFile=$SCDIR/deploy-ctr3a.shell

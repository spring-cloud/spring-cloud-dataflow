#!/usr/bin/env bash
if [ -z "$BASH_VERSION" ]; then
    echo "This script requires Bash. Use: bash $0 $*"
    exit 0
fi
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")

cat > run-ctr3a.shell <<EOF
task launch --name timestamp-ctr3a --properties app.composed-task-runner.logging.level.root=debug
EOF
"$SCDIR/shell.sh" --spring.shell.commandFile=run-ctr3a.shell

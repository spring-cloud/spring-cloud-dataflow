#!/usr/bin/env bash
if [ -z "$BASH_VERSION" ]; then
    echo "This script requires Bash. Use: bash $0 $*"
    exit 0
fi
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")

cat > deploy-ts-task.shell <<EOF
task create --name ts-task --definition "timestamp"
task launch --name ts-task --properties app.*.logging.level.root=debug
EOF
"$SCDIR/shell.sh" --spring.shell.commandFile=deploy-ts-task.shell

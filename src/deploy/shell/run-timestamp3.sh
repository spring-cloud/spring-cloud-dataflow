#!/usr/bin/env bash
if [ -z "$BASH_VERSION" ]; then
    echo "This script requires Bash. Use: bash $0 $*"
    exit 0
fi
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")

cat > run-ts-task3.shell <<EOF
task launch --name ts-task3 --properties app.*.logging.level.root=debug
EOF

"$SCDIR/shell.sh" --spring.shell.commandFile=run-ts-task3.shell

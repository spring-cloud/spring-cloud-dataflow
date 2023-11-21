#!/usr/bin/env bash
if [ -z "$BASH_VERSION" ]; then
    echo "This script requires Bash. Use: bash $0 $*"
    exit 0
fi
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")

cat > deploy-ts-batch3.shell <<EOF
task create --name ts-batch3 --definition "timestamp-batch3"
task launch --name ts-batch3 --properties app.*.logging.level.root=debug
EOF

"$SCDIR/shell.sh" --spring.shell.commandFile=deploy-ts-batch3.shell

#!/usr/bin/env bash
if [ -z "$BASH_VERSION" ]; then
    echo "This script requires Bash. Use: bash $0 $*"
    exit 0
fi
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")

cat > deploy-timelogger.shell <<EOF
stream create --name timelogger --definition "time | log"
stream deploy --name timelogger --properties app.*.logging.level.root=debug
EOF

"$SCDIR/shell.sh" --spring.shell.commandFile=deploy-timelogger.shell

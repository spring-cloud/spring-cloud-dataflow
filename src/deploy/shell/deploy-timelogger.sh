#!/usr/bin/env bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")

cat > timelogger.shell <<EOF
stream create --name timelogger --definition "time | log"
stream deploy --name timelogger --properties app.*.logging.level.root=debug
EOF

$SCDIR/shell.sh --spring.shell.commandFile=timelogger.shell

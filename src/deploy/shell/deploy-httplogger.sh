#!/usr/bin/env bash
if [ -z "$BASH_VERSION" ]; then
    echo "This script requires Bash. Use: bash $0 $*"
    exit 0
fi
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")

cat > deploy-httplogger.shell <<EOF
stream create --name httplogger --definition "http | log"
stream deploy --name httplogger --properties app.*.logging.level.root=debug,deployer.http.kubernetes.createLoadBalancer=true
EOF

"$SCDIR/shell.sh" --spring.shell.commandFile=deploy-httplogger.shell

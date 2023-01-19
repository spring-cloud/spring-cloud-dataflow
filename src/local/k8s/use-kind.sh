#!/bin/bash
#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
source $SCDIR/use-mk.sh kind $*

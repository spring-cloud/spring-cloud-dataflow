#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
DIR=$1
echo "<testsuite>" > combined.xml
set -e
find "$1" -name "*.xml" -exec $SCDIR/combine-fragment.sh "$SCDIR/combine-testcases.xsl" '{}' \; 2> /dev/null >> combined.xml
echo "</testsuite>" >> combined.xml
xsltproc $SCDIR/extract-failures.xsl combined.xml

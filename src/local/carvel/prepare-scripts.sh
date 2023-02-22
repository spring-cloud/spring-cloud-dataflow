#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
pushd $SCDIR/scripts > /dev/null
find . -type f -name "*.sh" -exec bash -c "cp -f '{}' '../{}.txt' && sed -i 's/\\$/\\\\$/g' '../{}.txt'" \;
find . -type f -name "*.yml" -exec bash -c "cp -f '{}' '../{}.txt'" \;
find . -type f -exec zip ../scdf-carvel.zip '{}' \;
popd > /dev/null

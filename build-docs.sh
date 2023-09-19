#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)
pushd "$SCDIR" > /dev/null || exit
    ./mvnw install -DskipTests -am -pl :spring-cloud-dataflow-classic-docs,:spring-cloud-dataflow-docs,:spring-cloud-skipper-server-core,:spring-cloud-skipper-docs -Pfull,asciidoctordocs,restdocs
popd > /dev/null || exit

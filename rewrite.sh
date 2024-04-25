#!/bin/bash
rm -f rewrite.log
CMD=$1
shift
RECIPES="1 2 3"
if [ "$1" != "" ]; then
    RECIPES=
fi
while [ "$1" != "" ]; do
    RECIPES="$RECIPES $1"
    shift
done
for RECIPE in $RECIPES; do
    case $RECIPE in
    "1")
        OPT="-Drewrite.activeRecipes=org.openrewrite.java.testing.hamcrest.MigrateHamcrestToAssertJ -Drewrite.recipeArtifactCoordinates=org.openrewrite.recipe:rewrite-testing-frameworks:2.6.0"
        ;;
    "2")
        OPT="-Drewrite.recipeArtifactCoordinates=org.openrewrite.recipe:rewrite-testing-frameworks:RELEASE -Drewrite.activeRecipes=org.openrewrite.java.testing.assertj.JUnitAssertThrowsToAssertExceptionType"
        ;;
    "3")
        OPT="-Drewrite.activeRecipes=org.openrewrite.java.spring.boot2.SpringBoot2JUnit4to5Migration -Drewrite.recipeArtifactCoordinates=org.openrewrite.recipe:rewrite-spring:5.7.0"
        ;;
    *)
        echo "Unknown recipe $RECIPE"
        exit 1
        ;;
    esac
    find . -name pom.xml -type f -exec mvn org.openrewrite.maven:rewrite-maven-plugin:$CMD $OPT -N -f '{}' \; | tee -a rewrite.log
done

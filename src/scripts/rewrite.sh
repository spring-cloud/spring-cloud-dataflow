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
        RECIPE_CLASS="org.openrewrite.java.testing.hamcrest.MigrateHamcrestToAssertJ"
        RECIPE_COORD="org.openrewrite.recipe:rewrite-testing-frameworks:RELEASE"
        ;;
    "2")
        RECIPE_CLASS="org.openrewrite.java.testing.assertj.JUnitToAssertj"
        RECIPE_COORD="org.openrewrite.recipe:rewrite-testing-frameworks:RELEASE"
        ;;
    "3")
        RECIPE_CLASS="org.openrewrite.java.spring.boot2.SpringBoot2JUnit4to5Migration"
        RECIPE_COORD="org.openrewrite.recipe:rewrite-spring:RELEASE"
        ;;
    *)
        echo "Unknown recipe $RECIPE"
        exit 1
        ;;
    esac
    find . -name pom.xml -type f -exec mvn org.openrewrite.maven:rewrite-maven-plugin:$CMD -Drewrite.activeRecipes="$RECIPE_CLASS" -Drewrite.recipeArtifactCoordinates="$RECIPE_COORD" -N -f '{}' \; | tee -a rewrite.log
done

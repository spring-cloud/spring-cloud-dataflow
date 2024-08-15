#!/bin/bash
if [ "$2" = "" ]; then
    echo "Usage $0 <module folder> <rewrite-command> [recipes]"
    exit 1
fi
if [[ "$1" == *"pom.xml" ]]; then
    MODULE_DIR=$(realpath $(dirname "$1"))
else
    MODULE_DIR=$(realpath "$1")
fi
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
ROOT_DIR=$(realpath "$SCDIR/../..")
CMD="$2"
shift
shift
RECIPES=""
COUNT_HAMCREST=$(grep -c -F "hamcrest" pom.xml)
if ((COUNT_HAMCREST>0)); then
    RECIPES="2"
fi
if [ "$RECIPES" = "" ]; then
    RECIPES="$RECIPES 1 3"
fi
if [ "$1" != "" ]; then
    RECIPES="$1"
    shift
fi
while [ "$1" != "" ]; do
    RECIPES="$RECIPES $1"
    shift
done
echo "RECIPES=$RECIPES"
for RECIPE in $RECIPES; do
    RECIPE_ARGS=
    case $RECIPE in
    "1")
        RECIPE_CLASS="org.openrewrite.java.testing.assertj.Assertj"
        RECIPE_COORD="org.openrewrite.recipe:rewrite-testing-frameworks:RELEASE"
        ;;
    "2")
        RECIPE_CLASS="org.openrewrite.java.testing.hamcrest.MigrateHamcrestToAssertJ"
        RECIPE_COORD="org.openrewrite.recipe:rewrite-testing-frameworks:RELEASE"
        ;;
    "3")
        RECIPE_CLASS="org.openrewrite.java.testing.testcontainers.TestContainersBestPractices"
        RECIPE_COORD="org.openrewrite.recipe:rewrite-testing-frameworks:RELEASE"
        RECIPE_ARGS="$RECIPE_ARGS -Drewrite.exportDatatables=true"
        ;;
    "4")
        RECIPE_CLASS="org.openrewrite.java.spring.boot2.SpringBoot2JUnit4to5Migration"
        RECIPE_COORD="org.openrewrite.recipe:rewrite-spring:RELEASE"
        ;;
    "5")
        # RECIPE_CLASS="org.openrewrite.java.spring.boot2.SpringBoot2JUnit4to5Migration"
        RECIPE_CLASS="org.openrewrite.java.testing.junit5.JUnit5BestPractices"
        RECIPE_COORD="org.openrewrite.recipe:rewrite-testing-frameworks:RELEASE"
        # RECIPE_COORD="org.openrewrite.recipe:rewrite-spring:RELEASE"
        ;;
    *)
        echo "Unknown recipe $RECIPE"
        exit 1
        ;;
    esac
    echo "Command:$CMD, Recipe:$RECIPE_CLASS in $MODULE_DIR"
    pushd "$MODULE_DIR" > /dev/null
        $ROOT_DIR/mvnw -s $ROOT_DIR/.settings.xml org.openrewrite.maven:rewrite-maven-plugin:$CMD -Drewrite.activeRecipes="$RECIPE_CLASS" -Drewrite.recipeArtifactCoordinates="$RECIPE_COORD" $RECIPE_ARGS $MAVEN_ARGS -N -f . | tee ${MODULE_DIR}/rewrite.log
        RC=$?
        ERRORS=$(grep -c -F ERROR ${MODULE_DIR}/rewrite.log)
        rm -f ${MODULE_DIR}/rewrite.log
        if ((ERRORS>0)) && ((RC > 0)); then
            echo "MODULE=$MODULE_DIR, RC=$RC, ERRORS=$ERRORS"
            exit $RC
        fi
    popd > /dev/null
done

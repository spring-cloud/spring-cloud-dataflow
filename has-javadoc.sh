#!/usr/bin/env bash
COUNT_FOUND=0
COUNT_MISSING_JAR=0
COUNT_MISSING_JAVADOC=0
COUNT_MISSING_SOURCES=0

function check_jars() {
    TARGET=$1
    TARGET_DIR=$2
    VERSION=$3
    set +e
    RESULT=$(ls -1a "$TARGET_DIR/" 2> /dev/null)
    if [[ "$RESULT" != *"-$VERSION-javadoc.jar"* ]]; then
        COUNT_MISSING_JAVADOC=$((1 + COUNT_MISSING_JAVADOC))
        echo "No javadoc for $TARGET_DIR"
    fi
    if [[ "$RESULT" != *"-$VERSION-sources.jar"* ]]; then
        COUNT_MISSING_SOURCES=$((1 + COUNT_MISSING_SOURCES))
        echo "No sources for $TARGET_DIR"
    fi
}
VERSION=$(./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout)
FILES=$(find . -name "pom.xml" -type f)
MVNV=$(realpath ./mvnw)
for file in $FILES; do
    if [[ "$file" != *"/target/"* ]]; then
        COUNT_FOUND=$((1 + COUNT_FOUND))
        DIR=$(dirname "$file")
        pushd "$DIR" > /dev/null || exit 1
            PACKAGING=$($MVNV help:evaluate -Dexpression=project.packaging -q -DforceStdout)
        popd > /dev/null || exit 1
        if [ "$PACKAGING" == "jar" ]; then
            FILE=$(find $DIR/target -name "*-${VERSION}.jar" 2> /dev/null)
            if [ "$FILE" = "" ]; then
                COUNT_MISSING_JAR=$((1 + COUNT_MISSING_JAR))
                echo "No jar in $DIR/target $PACKAGING"
            else
                check_jars "$FILE" "$DIR/target" "$VERSION"
            fi
        fi
    fi
done
echo "Found JAR Modules $COUNT_FOUND"
echo "Missing jars $COUNT_MISSING_JAR"
echo "Missing javadoc $COUNT_MISSING_JAVADOC"
echo "Missing sources $COUNT_MISSING_SOURCES"

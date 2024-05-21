#!/usr/bin/env bash
RUNNER_TMP="$1"
DEPLOYER_VERSION="$2"
DATAFLOW_UI_VERSION="$3"
DATAFLOW_VERSION="$4"
if [ "$4" == "" ]; then
    echo "Usage: <temp-folder> <deployer-version> <dataflow-ui-version> <dataflow-version>"
    exit 1
fi
RELEASE_NOTES_FILE="$RUNNER_TMP/release_notes.md"
RELEASE_NOTES_DATA="$RUNNER_TMP/release_notes_data.json"
RELEASE_NOTES_HEADERS1="$RUNNER_TMP/release_notes_headers1.json"
RELEASE_NOTES_HEADERS2="$RUNNER_TMP/release_notes_headers2.json"
RELEASE_NOTES_HEADERS3="$RUNNER_TMP/release_notes_headers3.json"
RELEASE_NOTES_FOOTERS1="$RUNNER_TMP/release_notes_footers1.json"
RELEASE_NOTES_FOOTERS2="$RUNNER_TMP/release_notes_footers2.json"
RELEASE_NOTES_FOOTERS3="$RUNNER_TMP/release_notes_footers3.json"
RELEASE_NOTES_ISSUES1="$RUNNER_TMP/release_notes_issues1.json"
RELEASE_NOTES_ISSUES2="$RUNNER_TMP/release_notes_issues2.json"
RELEASE_NOTES_ISSUES3="$RUNNER_TMP/release_notes_issues3.json"
RELEASE_NOTES_PROJECT1="$RUNNER_TMP/release_notes_project1.json"
RELEASE_NOTES_PROJECT2="$RUNNER_TMP/release_notes_project2.json"
RELEASE_NOTES_PROJECT3="$RUNNER_TMP/release_notes_project3.json"
echo "Retrieving headers"
gh issue list --repo spring-cloud/spring-cloud-deployer \
    --search milestone:$DEPLOYER_VERSION \
    --label automation/rlnotes-header  \
    --state all --json title,body \
    --jq '{headers:map(.),headerslength:(length)}' \
    > $RELEASE_NOTES_HEADERS1
gh issue list --repo spring-cloud/spring-cloud-dataflow-ui \
    --search milestone:$DATAFLOW_UI_VERSION \
    --label automation/rlnotes-header  \
    --state all --json title,body \
    --jq '{headers:map(.),headerslength:(length)}' \
    > $RELEASE_NOTES_HEADERS2
gh issue list --repo spring-cloud/spring-cloud-dataflow \
    --search milestone:$DATAFLOW_VERSION \
    --label automation/rlnotes-header  \
    --state all --json title,body \
    --jq '{headers:map(.),headerslength:(length)}' \
    > $RELEASE_NOTES_HEADERS3
echo "Retrieving footers"
gh issue list --repo spring-cloud/spring-cloud-deployer \
    --search milestone:$DEPLOYER_VERSION \
    --label automation/rlnotes-footer  \
    --state all --json title,body \
    --jq '{footers:map(.),footerslength:(length)}' \
    > $RELEASE_NOTES_FOOTERS1
gh issue list --repo spring-cloud/spring-cloud-dataflow-ui \
    --search milestone:$DATAFLOW_UI_VERSION \
    --label automation/rlnotes-footer  \
    --state all --json title,body \
    --jq '{footers:map(.),footerslength:(length)}' \
    > $RELEASE_NOTES_FOOTERS2
gh issue list --repo spring-cloud/spring-cloud-dataflow \
    --search milestone:$DATAFLOW_VERSION \
    --label automation/rlnotes-footer  \
    --state all --json title,body \
    --jq '{footers:map(.),footerslength:(length)}' \
    > $RELEASE_NOTES_FOOTERS3
echo "Creating project data"
echo "{\"name\":\"Spring Cloud Dataflow Deployer\",\"version\":\"$DEPLOYER_VERSION\"}" > $RELEASE_NOTES_PROJECT1
echo "{\"name\":\"Spring Cloud Dataflow UI\",\"version\":\"$DATAFLOW_UI_VERSION\"}" > $RELEASE_NOTES_PROJECT2
echo "{\"name\":\"Spring Cloud Dataflow\",\"version\":\"$DATAFLOW_VERSION\"}" > $RELEASE_NOTES_PROJECT3

echo "Retrieving issues"
gh issue list --repo spring-cloud/spring-cloud-deployer \
    --search milestone:$DEPLOYER_VERSION \
    --state all --json number,title,labels \
    --jq '{issues:map(select((.labels | length == 0) or (any(.labels[].name; startswith("automation/rlnotes")|not))) + {repo:"spring-cloud/spring-cloud-deployer"})}' \
    > $RELEASE_NOTES_ISSUES1
gh issue list --repo spring-cloud/spring-cloud-dataflow-ui \
    --search milestone:$DATAFLOW_UI_VERSION \
    --state all --json number,title,labels \
    --jq '{issues:map(select((.labels | length == 0) or (any(.labels[].name; startswith("automation/rlnotes")|not))) + {repo:"spring-cloud/spring-cloud-dataflow-ui"})}' \
    > $RELEASE_NOTES_ISSUES2
gh issue list --repo spring-cloud/spring-cloud-dataflow \
    --search milestone:$DATAFLOW_VERSION \
    --state all --limit 100 --json number,title,labels \
    --jq '{issues:map(select((.labels | length == 0) or (any(.labels[].name; startswith("automation/rlnotes")|not))) + {repo:"spring-cloud/spring-cloud-dataflow"})}' \
    > $RELEASE_NOTES_ISSUES3
echo "Creating release notes data"
jq -s '{issues:(.[0].issues + .[1].issues + .[2].issues),headers:(.[3].headers + .[4].headers + .[5].headers),headerslength:(.[3].headerslength + .[4].headerslength + .[5].headerslength),footers:(.[6].footers + .[7].footers + .[8].footers), footerslength:(.[6].footerslength + .[7].footerslength + .[8].footerslength),projects:{spring_cloud_deployer:{name:"Spring Cloud Deployer",version:(.[9].version)},spring_cloud_skipper:{name:"Spring Cloud Skipper",version:(.[11].version)},spring_cloud_dataflow_ui:{name:"Spring Cloud Dataflow UI",version:(.[10].version)},spring_cloud_dataflow:{name:"Spring Cloud Dataflow",version:(.[11].version)}}}' \
    $RELEASE_NOTES_ISSUES1 $RELEASE_NOTES_ISSUES2 $RELEASE_NOTES_ISSUES3 \
    $RELEASE_NOTES_HEADERS1 $RELEASE_NOTES_HEADERS2 $RELEASE_NOTES_HEADERS3 \
    $RELEASE_NOTES_FOOTERS1 $RELEASE_NOTES_FOOTERS2 $RELEASE_NOTES_FOOTERS3 \
    $RELEASE_NOTES_PROJECT1 $RELEASE_NOTES_PROJECT2 $RELEASE_NOTES_PROJECT3 \
> $RELEASE_NOTES_DATA
echo "Applying mustache templates"
mustache $RELEASE_NOTES_DATA .github/rlnotes.mustache > $RELEASE_NOTES_FILE

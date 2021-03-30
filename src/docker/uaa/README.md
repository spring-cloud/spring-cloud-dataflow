# Test image for uaa

This image contains hardcoded values for uaa server which we use
with integration tests to see that basic oauth setup works.

Config autoapproves all dataflow scopes where user `janne` with
password `janne` has all scopes and user `guest` with password
`guest` only `dataflow.view` scope.

There are build scripts to build uaa and docker and must be
build with jdk8 as that uaa version is rather old stuff.

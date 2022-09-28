# Local Development Support

These scripts are useful when you want to build the container on a different architecture and publish to a private repo.

## `download-app.sh`
Downloads all applications needed by `create-containers.sh` from Maven repository.

*If the timestamp of snapshots matches the download will be skipped.*

Usage: `download-app.sh [version]`
* `version` is the skipper version like `2.8.6` or default is `2.9.0-SNAPSHOT`

## `create-container.sh`
Creates all containers and pushes to local docker registry.

This script requires [jib-cli](https://github.com/GoogleContainerTools/jib/tree/master/jib-cli)

Usage: `create-containers.sh [version] [jre-version]`
* `version` is the skipper version like `2.8.6` or default is `2.9.0-SNAPSHOT`
* `jre-version` should be one of 11, 17

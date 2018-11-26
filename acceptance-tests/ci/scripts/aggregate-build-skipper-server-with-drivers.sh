#!/bin/sh

for DIR in `ls | grep distribution-repository-`; do cp -R $DIR/* distribution-repository; done


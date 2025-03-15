#!/bin/bash

set -e

NOW=$(date +'%Y%m%d-%H%M%S')
SHORT_SHA=$(git rev-parse --short HEAD)
IMAGE_TAG="${NOW}.${SHORT_SHA}"

echo $IMAGE_TAG
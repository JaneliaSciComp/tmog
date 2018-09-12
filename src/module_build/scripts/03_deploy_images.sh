#!/bin/bash

DEPLOY_HOST="trautmane-ws1"

set -e

ABSOLUTE_SCRIPT=`readlink -m $0`
SCRIPTS_DIR=`dirname ${ABSOLUTE_SCRIPT}`
TMOG_BUILD_DIR=`readlink -m ${SCRIPTS_DIR}/../../../build`
JLINK_IMAGES_DIR="${TMOG_BUILD_DIR}/jlink_images"

DATE=`date +"%Y%m%d_%H%M%S"`
IMAGES_DATE_DIR="images_${DATE}"

function deployImages {

  local DEPLOY_DIR="${1}/deploy/${IMAGES_DATE_DIR}"
  shift 1

  echo """
    deploying images to ${DEPLOY_HOST}:${DEPLOY_DIR} ... """

  scp -rp ${JLINK_IMAGES_DIR} ${DEPLOY_HOST}:${DEPLOY_DIR}

  #ssh ${DEPLOY_HOST} "chmod 664 ${DEPLOY_DIR}/*.xml"

}

# deploy fly images to appropriate areas
deployImages /groups/flylight/flylight/tmog
#deployImages /groups/projtechres/projtechres/tmog
#deployImages /groups/flyfuncconn/flyfuncconn/tmog

# deploy to central location for others?

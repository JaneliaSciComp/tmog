#!/bin/bash

DEPLOY_HOST="trautmane-ws1"

set -e

ABSOLUTE_SCRIPT=`readlink -m $0`
SCRIPTS_DIR=`dirname ${ABSOLUTE_SCRIPT}`
TMOG_BUILD_DIR=`readlink -m ${SCRIPTS_DIR}/../../../build`
PACKAGES_DIR="${TMOG_BUILD_DIR}/packages"

DATE_TAG=`date +"%Y%m%d_%H%M%S"`

function deployPackages {

  local DEPLOY_PARENT_DIR="${1}/deploy/app"
  local DATED_DIR="${DEPLOY_PARENT_DIR}/${DATE_TAG}"
  local CURRENT_DIR="${DEPLOY_PARENT_DIR}/current"
  shift 1

  ssh ${DEPLOY_HOST} "mkdir -p ${DATED_DIR}"

  echo """
    deploying images to ${DEPLOY_HOST}:${DATED_DIR} ... """

  scp -rp ${PACKAGES_DIR}/* ${DEPLOY_HOST}:${DATED_DIR}
  ssh ${DEPLOY_HOST} "rm -rf ${CURRENT_DIR}; mkdir -p ${CURRENT_DIR}; cp -r ${DATED_DIR}/* ${CURRENT_DIR}"

}

deployPackages /groups/flyfuncconn/flyfuncconn/tmog
deployPackages /groups/flylight/flylight/tmog
deployPackages /groups/projtechres/projtechres/tmog

#deployPackages /groups/leet/leetimg/leetlab/tmog
#deployPackages /groups/magee/mageelab/tmog
#deployPackages /groups/rubin/data1/rubinlab/tmog
#deployPackages /groups/svoboda/wdbp/tmog
#deployPackages /groups/zlatic/zlaticlab/tmog
#deployPackages /nrs/zlatic/zlaticlab/tmog

#!/bin/bash

DEPLOY_HOST="trautmane-ws1"

set -e

ABSOLUTE_SCRIPT=`readlink -m $0`
SCRIPTS_DIR=`dirname ${ABSOLUTE_SCRIPT}`
TMOG_BUILD_DIR=`readlink -m ${SCRIPTS_DIR}/../../../build`
RESOURCES_DIR="${TMOG_BUILD_DIR}/resources/main"

DATE=`date +"%Y%m%d_%H%M%S"`
CONFIG_DATE_DIR="config_${DATE}"

function deployConfig {

  local DEPLOY_DIR="${1}/deploy/${CONFIG_DATE_DIR}"
  shift 1

  ssh ${DEPLOY_HOST} "mkdir -p ${DEPLOY_DIR}"

  if (( $# > 0 )); then

    echo """
      deploying $# config file(s) to ${DEPLOY_HOST}:${DEPLOY_DIR} ... """

    for CONFIG in $*; do
      scp -p ${RESOURCES_DIR}/transmogrifier_config_${CONFIG}.xml ${DEPLOY_HOST}:${DEPLOY_DIR}
    done

  else

    echo """
      deploying ALL config file(s) to ${DEPLOY_HOST}:${DEPLOY_DIR} ... """

    scp -p ${RESOURCES_DIR}/transmogrifier_config* ${DEPLOY_HOST}:${DEPLOY_DIR}

  fi

  ssh ${DEPLOY_HOST} "chmod 664 ${DEPLOY_DIR}/*.xml"

}

# deploy fly configs to appropriate areas
deployConfig /groups/flylight/flylight/tmog flylight_flip flylight_polarity flylight_test split_screen_review
deployConfig /groups/projtechres/projtechres/tmog projtechres
deployConfig /groups/flyfuncconn/flyfuncconn/tmog flyfuncconn

# deploy everything to central location for later individual distribution (as needed)
deployConfig /groups/scicompsoft/home/trautmane/tmog

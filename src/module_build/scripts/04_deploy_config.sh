#!/bin/bash

DEPLOY_HOST="c13u01.int.janelia.org"

set -e

ABSOLUTE_SCRIPT=`readlink -m $0`
SCRIPTS_DIR=`dirname ${ABSOLUTE_SCRIPT}`
SRC_RESOURCES_DIR=`readlink -m ${SCRIPTS_DIR}/../../main/resources`

DATE_TAG=`date +"%Y%m%d_%H%M%S"`

function deployConfig {

  local DEPLOY_PARENT_DIR="${1}/deploy/config"
  local DATED_DIR="${DEPLOY_PARENT_DIR}/${DATE_TAG}"
  local CURRENT_DIR="${DEPLOY_PARENT_DIR}/current"

  shift 1

  ssh ${DEPLOY_HOST} "mkdir -p ${DATED_DIR}"

  if (( $# > 0 )); then

    echo """
      deploying $# config file(s) to ${DEPLOY_HOST}:${DATED_DIR} ... """

    for CONFIG_NAME in $*; do
      scp -p ${SRC_RESOURCES_DIR}/transmogrifier_config_${CONFIG_NAME}.xml ${DEPLOY_HOST}:${DATED_DIR}
    done

    # update current config copy
    ssh ${DEPLOY_HOST} "rm -rf ${CURRENT_DIR}; mkdir -p ${CURRENT_DIR}; cp -r ${DATED_DIR}/* ${CURRENT_DIR}; chmod 664 ${CURRENT_DIR}/*.xml"

  else

    echo """
      deploying ALL config file(s) to ${DEPLOY_HOST}:${DATED_DIR} ... """

    scp -p ${SRC_RESOURCES_DIR}/transmogrifier_config* ${DEPLOY_HOST}:${DATED_DIR}

  fi

}

# deploy configs to appropriate areas
#deployConfig /groups/flylight/flylight/tmog flylight_flip flylight_polarity flylight_test split_screen_review
#deployConfig /groups/projtechres/projtechres/tmog projtechres

# legacy (to be migrated from java web start) configs:
#deployConfig /groups/leet/leetimg/leetlab/tmog leet_lineage
#deployConfig /groups/magee/mageelab/tmog magee_simon_linker
#deployConfig /groups/svoboda/wdbp/tmog svoboda_wdbp_mainwc svoboda_wdbp_simon_linker wdbp
#deployConfig /groups/zlatic/zlaticlab/tmog zlatic zlatic_closed_loop zlatic_odor
#deployConfig /nrs/zlatic/zlaticlab/tmog zlatic_fpga

# deploy everything to central location for later individual distribution (as needed)
deployConfig /groups/scicompsoft/home/trautmane/tmog

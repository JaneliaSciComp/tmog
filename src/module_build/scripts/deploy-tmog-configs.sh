#!/bin/bash

# This script copies transmogrifier config files from a specified
# timestamped config directory to the flylight shared config directory.
# It is currently deployed in /groups/scicompsoft/home/trautmane/bin

set -e

# /groups/scicompsoft/home/trautmane/tmog/deploy/config/20190411_175550

CONFIG_TS="$1"

SOURCE_CONFIG_DIR="/groups/scicompsoft/home/trautmane/tmog/deploy/config/${CONFIG_TS}"

if [[ ! -f ${SOURCE_CONFIG_DIR}/transmogrifier_config.xsd ]]; then
  echo """
ERROR: invalid source config directory ${SOURCE_CONFIG_DIR}
"""
  exit 1
fi

FLY_LIGHT_DIR="/groups/flylight/flylight/tmog/config"
for CONFIG in flylight_flip flylight_polarity flylight_test split_screen_review projtechres; do
  cp ${SOURCE_CONFIG_DIR}/transmogrifier_config_${CONFIG}.xml ${FLY_LIGHT_DIR}
done
ls -al ${FLY_LIGHT_DIR}/*.xml
echo
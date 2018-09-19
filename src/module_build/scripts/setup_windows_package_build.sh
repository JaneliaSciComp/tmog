#!/bin/bash

# This script sets up a windows_package_build directory that can be be easily copied to a Windows VM
# to facilitate running the javapackager tool (see package_windows.bat) on Windows.

set -e

JDK_VERSION="jdk-10.0.2_windows-x64"
JDK_DIR="${HOME}/windows_jdks/${JDK_VERSION}"

if [[ ! -d ${JDK_DIR} ]]; then
  echo """
    ERROR: Missing JDK in ${JDK_DIR}
    See src/module_build/scripts/download_windows_jdk_to_mac.sh for details on how to download.
  """
  exit 1
fi

ABSOLUTE_SCRIPT=`readlink -m $0`
SCRIPTS_DIR=`dirname ${ABSOLUTE_SCRIPT}`
TMOG_BUILD_DIR=`readlink -m ${SCRIPTS_DIR}/../../../build`

TMOG_LIBS_DIR="${TMOG_BUILD_DIR}/libs_prod"
FIXED_LIBS_DIR="${TMOG_BUILD_DIR}/libs_fixed"
PACKAGE_BUILD_DIR="${TMOG_BUILD_DIR}/windows_package_build"
BAT_FILE="package_windows.bat"
PACKAGE_RESOURCES_DIR="${PACKAGE_BUILD_DIR}/package/windows"
IC0_FILE="${TMOG_BUILD_DIR}/resources/main/images/tmog.ico"

rm -rf ${PACKAGE_BUILD_DIR}/*
mkdir -p ${PACKAGE_RESOURCES_DIR}

# copy jdk and everything we've built to the windows_package_build directory
cp -r ${JDK_DIR} ${TMOG_LIBS_DIR} ${FIXED_LIBS_DIR} ${SCRIPTS_DIR}/package_windows.bat ${PACKAGE_BUILD_DIR}

# copy icon to special location to work around javapackager bug/feature
# ( see https://bugs.openjdk.java.net/browse/JDK-8200382 )
cp ${TMOG_BUILD_DIR}/resources/main/images/tmog.ico ${PACKAGE_RESOURCES_DIR}

# set the specific jdk version (path) in the package script
sed "s/JDK_VERSION_FROM_SETUP/${JDK_VERSION}/g"  ${SCRIPTS_DIR}/${BAT_FILE} > ${PACKAGE_BUILD_DIR}/${BAT_FILE}

echo """
Everything needed for windows packaging has been copied to:
${PACKAGE_RESOURCES_DIR}

Transfer that directory to a Windows VM (or box) and run package_windows.bat.
"""
#!/bin/bash

set -e

MAC_JAVA_HOME="/Library/Java/JavaVirtualMachines/open/jdk-10.0.2.jdk/Contents/Home"
WINDOWS_JDK="${HOME}/windows_jdks/jdk-10.0.2_windows-x64"


ABSOLUTE_SCRIPT=`readlink -m $0`
SCRIPTS_DIR=`dirname ${ABSOLUTE_SCRIPT}`
MODULE_BUILD_DIR=`readlink -m ${SCRIPTS_DIR}/..`
TMOG_BUILD_DIR=`readlink -m ${SCRIPTS_DIR}/../../../build`

TMOG_LIBS_DIR="${TMOG_BUILD_DIR}/libs_prod"
FIXED_LIBS_DIR="${TMOG_BUILD_DIR}/libs_fixed"
PACKAGES_DIR="${TMOG_BUILD_DIR}/packages"

mkdir -p ${PACKAGES_DIR}
rm -rf ${PACKAGES_DIR}/*

TMOG_MODULE="org.janelia.tmog"
TMOG_MAIN="${TMOG_MODULE}/org.janelia.it.ims.tmog.JaneliaTransmogrifier"

# ------------------------------------------------------
# For now, skip linking Mac runtime image and just package it directly since builds will be run on Mac.

#OUT_MAC_DIR="${JLINK_IMAGES_DIR}/mac"

#echo "assembling mac runtime image into ${OUT_MAC_DIR}"
#${MAC_JAVA_HOME}/bin/jlink --output ${OUT_MAC_DIR} --module-path ${TMOG_LIBS_DIR}:${FIXED_LIBS_DIR} --add-modules ${TMOG_MODULE} --launcher command=${TMOG_MAIN}

echo """
================================================================
Packaging mac runtime image into ${PACKAGES_DIR}/mac ...
"""
${SCRIPTS_DIR}/package_mac.sh


# ------------------------------------------------------
# Link windows image so that it can be "plugged-in" to package template.
# This allows everything to be built on a Mac.
# A Windows box build is only needed if the packaging template needs to change.

# From https://stackoverflow.com/questions/47593409/create-java-runtime-image-on-one-platform-for-another-using-jlink
#   The jlink tool can create a run-time image for another platform (cross targeting).
#   You need to download two JDKs to do this.
#   One for the platform where you run jlink, the other for the target platform.
#   Run jlink with --module-path $TARGET/jmods where $TARGET is the directory where you've unzipped the JDK for the target platform.

# see download_windows_jdk_to_mac.sh for details on downloading Windows JDK to Mac

WINDOWS_PACKAGE_DIR="${PACKAGES_DIR}/windows"
echo """
================================================================
Assembling windows runtime image into ${WINDOWS_PACKAGE_DIR} ...
"""

cp -r ${MODULE_BUILD_DIR}/package_templates/windows ${PACKAGES_DIR}
# template copy may have too restrictive file permissions so reset them here before deploying
chmod 750 ${WINDOWS_PACKAGE_DIR}/*.* ${WINDOWS_PACKAGE_DIR}/app/*.*

OUT_WINDOWS_DIR="${WINDOWS_PACKAGE_DIR}/runtime"
WINDOWS_JMODS="${WINDOWS_JDK}/jmods"

${MAC_JAVA_HOME}/bin/jlink --output ${OUT_WINDOWS_DIR} --module-path ${WINDOWS_JMODS}:${TMOG_LIBS_DIR}:${FIXED_LIBS_DIR} --add-modules ${TMOG_MODULE} --launcher command=${TMOG_MAIN}

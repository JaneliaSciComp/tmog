#!/bin/bash

set -e

MAC_JAVA_HOME="/Library/Java/JavaVirtualMachines/jdk-10.0.2.jdk/Contents/Home"
WINDOWS_JDK="${HOME}/windows_jdks/jdk-10.0.2_windows-x64"


ABSOLUTE_SCRIPT=`readlink -m $0`
SCRIPTS_DIR=`dirname ${ABSOLUTE_SCRIPT}`
TMOG_BUILD_DIR=`readlink -m ${SCRIPTS_DIR}/../../../build`


TMOG_JAR=`ls ${TMOG_BUILD_DIR}/libs/tmog-*-prod.jar`
FIXED_LIBS_DIR="${TMOG_BUILD_DIR}/libs_fixed"
JLINK_IMAGES_DIR="${TMOG_BUILD_DIR}/jlink_images"

mkdir -p ${JLINK_IMAGES_DIR}
rm -rf ${JLINK_IMAGES_DIR}/*

TMOG_MODULE="org.janelia.tmog"
TMOG_MAIN="${TMOG_MODULE}/org.janelia.it.ims.tmog.JaneliaTransmogrifier"

# ------------------------------------------------------
OUT_MAC_DIR="${JLINK_IMAGES_DIR}/mac"

echo "assembling mac runtime image into ${OUT_MAC_DIR}"
${MAC_JAVA_HOME}/bin/jlink --output ${OUT_MAC_DIR} --module-path ${TMOG_JAR}:${FIXED_LIBS_DIR} --add-modules ${TMOG_MODULE} --launcher command=${TMOG_MAIN}


# ------------------------------------------------------

# From https://stackoverflow.com/questions/47593409/create-java-runtime-image-on-one-platform-for-another-using-jlink
#   The jlink tool can create a run-time image for another platform (cross targeting).
#   You need to download two JDKs to do this.
#   One for the platform where you run jlink, the other for the target platform.
#   Run jlink with --module-path $TARGET/jmods where $TARGET is the directory where you've unzipped the JDK for the target platform.

# see download_windows_jdk_to_mac.sh for details on downloading Windows JDK to Mac

OUT_WINDOWS_DIR="${JLINK_IMAGES_DIR}/windows"
WINDOWS_JMODS="${WINDOWS_JDK}/jmods"

echo "assembling windows runtime image into ${OUT_WINDOWS_DIR}"
${MAC_JAVA_HOME}/bin/jlink --output ${OUT_WINDOWS_DIR} --module-path ${WINDOWS_JMODS}:${TMOG_JAR}:${FIXED_LIBS_DIR} --add-modules ${TMOG_MODULE} --launcher command=${TMOG_MAIN}

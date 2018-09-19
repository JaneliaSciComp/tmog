#!/bin/bash

set -e

MAC_JAVA_HOME="/Library/Java/JavaVirtualMachines/jdk-10.0.2.jdk/Contents/Home"

ABSOLUTE_SCRIPT=`readlink -m $0`
SCRIPTS_DIR=`dirname ${ABSOLUTE_SCRIPT}`
TMOG_BUILD_DIR=`readlink -m ${SCRIPTS_DIR}/../../../build`

TMOG_LIBS_DIR="${TMOG_BUILD_DIR}/libs_prod"
FIXED_LIBS_DIR="${TMOG_BUILD_DIR}/libs_fixed"
PACKAGES_DIR="${TMOG_BUILD_DIR}/packages"
ICNS_FILE="${TMOG_BUILD_DIR}/resources/main/images/tmog.icns"

mkdir -p ${PACKAGES_DIR}
rm -rf ${PACKAGES_DIR}/*

CORE_ARGS="-deploy -v -native image -name tmog -outdir ${PACKAGES_DIR}/mac -outfile tmog -title tmog"

TMOG_MODULE="org.janelia.tmog"
TMOG_MAIN="${TMOG_MODULE}/org.janelia.it.ims.tmog.JaneliaTransmogrifier"
MODULE_ARGS="--module-path ${TMOG_LIBS_DIR}:${FIXED_LIBS_DIR} --add-modules ${TMOG_MODULE} --module ${TMOG_MAIN}"

BUNDLER_ARGS="-BsignBundle=false -BjvmOptions=-Xms264m -BjvmOptions=-Xmx1024m -Bicon=${ICNS_FILE}"
MAC_BUNDLER_ARGS="-Bmac.CFBundleIdentifier=org.janelia.tmog"

# ------------------------------------------------------

${MAC_JAVA_HOME}/bin/javapackager ${CORE_ARGS} ${MODULE_ARGS} ${BUNDLER_ARGS} ${MAC_BUNDLER_ARGS}

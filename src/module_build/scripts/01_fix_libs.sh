#!/bin/bash

set -e

ABSOLUTE_SCRIPT=`readlink -m $0`
SCRIPTS_DIR=`dirname ${ABSOLUTE_SCRIPT}`
MODULE_BUILD_DIR=`readlink -m ${SCRIPTS_DIR}/..`
TMOG_BUILD_DIR=`readlink -m ${SCRIPTS_DIR}/../../../build`

ADD_MODULE_INFO="${SCRIPTS_DIR}/add_module_info_to_jar.sh"
ORIGINAL_LIBS_DIR="${MODULE_BUILD_DIR}/libs_original"
FIXED_LIBS_DIR="${TMOG_BUILD_DIR}/libs_fixed"

# if keep_existing option is specified and the libs_fixed directory exists, exit without doing anything
if [[ "$1" == "keep_existing" ]]; then
  if [[ -d ${FIXED_LIBS_DIR} ]]; then
    exit 0
  fi
fi

mkdir -p ${FIXED_LIBS_DIR}
rm -f ${FIXED_LIBS_DIR}/*.jar

# NOTE: fix order matters
${ADD_MODULE_INFO} log4j ${ORIGINAL_LIBS_DIR}/log4j-1.2.8.jar ${FIXED_LIBS_DIR}
${ADD_MODULE_INFO} mysql.connector.java ${ORIGINAL_LIBS_DIR}/mysql-connector-java-5.1.22-bin.jar ${FIXED_LIBS_DIR}
${ADD_MODULE_INFO} forms.rt ${ORIGINAL_LIBS_DIR}/forms_rt-7.0.3.jar ${FIXED_LIBS_DIR}
${ADD_MODULE_INFO} glazedlists.java15 ${ORIGINAL_LIBS_DIR}/glazedlists_java15-1.8.0.jar ${FIXED_LIBS_DIR}
${ADD_MODULE_INFO} gson ${ORIGINAL_LIBS_DIR}/gson-2.7.jar ${FIXED_LIBS_DIR}
${ADD_MODULE_INFO} commons.codec ${ORIGINAL_LIBS_DIR}/commons-codec-1.3.jar ${FIXED_LIBS_DIR}
${ADD_MODULE_INFO} commons.logging ${ORIGINAL_LIBS_DIR}/commons-logging-1.1.jar ${FIXED_LIBS_DIR} --add-modules log4j
${ADD_MODULE_INFO} commons.beanutils ${ORIGINAL_LIBS_DIR}/commons-beanutils.jar ${FIXED_LIBS_DIR}
${ADD_MODULE_INFO} commons.httpclient ${ORIGINAL_LIBS_DIR}/commons-httpclient-3.1.jar ${FIXED_LIBS_DIR}
${ADD_MODULE_INFO} swing.worker ${ORIGINAL_LIBS_DIR}/swing-worker-1.1.jar ${FIXED_LIBS_DIR}
${ADD_MODULE_INFO} commons.digester ${ORIGINAL_LIBS_DIR}/commons-digester-1.8.jar ${FIXED_LIBS_DIR}

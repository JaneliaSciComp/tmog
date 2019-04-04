#!/bin/bash

MAC_JAVA_HOME="/Library/Java/JavaVirtualMachines/open/jdk-10.0.2.jdk/Contents/Home"
echo "MAC_JAVA_HOME is ${MAC_JAVA_HOME}"
export PATH="${MAC_JAVA_HOME}/bin:${PATH}"

if (( $# < 3 )); then
  echo "USAGE: `basename $0` <module-name> <source-jar> <fixed-libs-dir> [--add-modules <module>]"
  exit 1
fi

MODULE_NAME="$1"
SOURCE_JAR="$2"
FIXED_LIBS_DIR="$3"
shift 3
ADD_MODULES="$*"

JAR_FILE=`basename ${SOURCE_JAR}`
TARGET_JAR="${FIXED_LIBS_DIR}/${JAR_FILE}"

if [[ ! -f ${SOURCE_JAR} ]]; then
  echo "ERROR: missing source jar: ${SOURCE_JAR}"
  exit 1
fi

ORIGINAL_LIBS_DIR=`dirname ${SOURCE_JAR}`
if [[ "${ORIGINAL_LIBS_DIR}" == "${FIXED_LIBS_DIR}" ]]; then
  echo "ERROR: same source and target jar: ${SOURCE_JAR}"
  exit 1
fi

echo """
===============================================================================================================
Fixing ${SOURCE_JAR} ...
"""

FIX_DIR="${FIXED_LIBS_DIR}/fix_${MODULE_NAME}"
HACKED_JAR="${FIX_DIR}/${JAR_FILE}"
CLASSES_DIR="${FIX_DIR}/classes"
WORK_DIR="${FIX_DIR}/work"

rm -rf ${FIX_DIR} ${TARGET_JAR}
mkdir -p ${CLASSES_DIR} ${WORK_DIR} 

# ------------------------------------------------------
# clean-up problems from source jar

# copy original jar into place
cp ${SOURCE_JAR} ${HACKED_JAR}
chmod 644 ${HACKED_JAR}

function removeMissing {

  MISSING=`jdeps --module-path ${FIXED_LIBS_DIR} --generate-module-info ${WORK_DIR} ${HACKED_JAR} | grep "not found" | sed 's@\.@/@g' | awk '{print $1 ".class"}' | sort -u`
  NUM_MISSING=`echo ${MISSING} | wc -w`

  if (( NUM_MISSING > 0 )); then
    zip -d ${HACKED_JAR} ${MISSING}
    echo "removed ${NUM_MISSING} classes from ${HACKED_JAR}"
  else
    echo "no missing dependencies in ${HACKED_JAR}"
  fi

  return ${NUM_MISSING}
}

for ATTEMPT in 1 2 3; do
  removeMissing
  NUM_REMOVED=$?
  if (( NUM_REMOVED == 0 )); then
    break
  fi
done

if (( NUM_REMOVED != 0 )); then
  echo "ERROR: ${NUM_REMOVED} missing dependencies remain in ${HACKED_JAR}"
  exit 1
fi

echo

# ------------------------------------------------------
# generate module for cleaned-up (hacked) jar

rm -rf ${WORK_DIR}/*
jdeps --module-path ${FIXED_LIBS_DIR} --generate-module-info ${WORK_DIR} ${HACKED_JAR}

echo
if [[ -f  ${WORK_DIR}/${MODULE_NAME}/module-info.java ]]; then
  cat ${WORK_DIR}/${MODULE_NAME}/module-info.java
else
  echo "ERROR: module file not generated: ${WORK_DIR}/${MODULE_NAME}/module-info.java"
  exit 1
fi

# ------------------------------------------------------
# build module

# extract jar
cd ${CLASSES_DIR}
jar xf ${HACKED_JAR}

# compile module-info.java (generated by gen.sh)
cd ${WORK_DIR}/${MODULE_NAME}
javac ${ADD_MODULES} --module-path ${MODULE_NAME}:${FIXED_LIBS_DIR} -d ${CLASSES_DIR} module-info.java

# update output jar
jar uf ${HACKED_JAR} -C ${CLASSES_DIR} module-info.class

mv ${HACKED_JAR} ${TARGET_JAR}

echo """
added module-info.class to ${TARGET_JAR}:
"""
jar tvf ${TARGET_JAR} | grep module-info.class

echo

rm -rf ${FIX_DIR}

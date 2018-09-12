#!/bin/bash

JDK_VERSION="jdk-10.0.2_windows-x64"
JDK_EXE_FILE="${JDK_VERSION}_bin.exe"

# URL for JDK 10
# This occasionally needs to be updated when Oracle moves things around.
# You can find latest Windows download link at:
# http://www.oracle.com/technetwork/java/javase/downloads/jdk10-downloads-4416644.html
JDK_URL="http://download.oracle.com/otn-pub/java/jdk/10.0.2+13/19aef61b38124481863b1413dce1855f/${JDK_EXE_FILE}"


JDK_DIR="${HOME}/windows_jdks/${JDK_VERSION}"

mkdir -p ${JDK_DIR}
rm -rf ${JDK_DIR}/*

cd ${JDK_DIR}

echo """
downloading ${JDK_URL} to 
${JDK_DIR}
"""
curl -o ${JDK_EXE_FILE} -j -k -L -H "Cookie: oraclelicense=accept-securebackup-cookie" ${JDK_URL}

# use 7zip (brew install p7zip) to extract tools.zip from Windows exe archive
echo """
unpacking ${JDK_EXE_FILE}
"""
7z x ${JDK_EXE_FILE} 

unzip -q tools.zip
rm ${JDK_EXE_FILE} tools.zip

echo """
Contents of ${JDK_DIR} are:
"""
ls -al ${JDK_DIR}

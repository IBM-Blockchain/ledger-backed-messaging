#!/bin/bash


TMP=$(mktemp -d)

jq -r '.fabric.connection' package.json > ${TMP}/connection.json
jq -r '{ type: "external", label: .fabric.label }' package.json > ${TMP}/metadata.json

cat ${TMP}/metadata.json

tar -C ${TMP} -zcf  ${TMP}/code.tar.gz connection.json
tar -C  ${TMP} -zcf  this.tgz code.tar.gz metadata.json

rm -rf ${TMP}
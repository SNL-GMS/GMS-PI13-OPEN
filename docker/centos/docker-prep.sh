#!/bin/bash -ex

if [ $(uname) == 'Darwin' ]; then
    if [ ! -x "$(command -v gsed)" ]; then echo "ERROR: gsed not available.  Run 'brew install gnu-sed' to install."; exit 1; fi
    SED=gsed 
else
    SED=sed 
fi

$SED $'/^# CentOS Base Template End$/{e cat CentOS-base.tmpl\n}' Dockerfile.base > Dockerfile

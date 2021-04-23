#!/bin/bash -ex

rsync -av ../common-src .
rsync -av ../typescript/common-src .
rsync -av ../python/common-src .

if [ $(uname) == 'Darwin' ]; then
    if [ ! -x "$(command -v gsed)" ]; then echo "ERROR: gsed not available.  Run 'brew install gnu-sed' to install."; exit 1; fi
    SED=gsed 
else
    SED=sed 
fi

$SED -e $'/^# CentOS Base Template End$/{e cat ../CentOS-base.tmpl\n}' \
     -e $'/^# Node Base Template End$/{e cat ../typescript/node-base.tmpl\n}' \
     -e $'/^# Python Builder Template End$/{e cat ../python/python-builder.tmpl\n}' \
     -e $'/^# Python Base Template End$/{e cat ../python/python-base.tmpl\n}' \
    Dockerfile.base > Dockerfile

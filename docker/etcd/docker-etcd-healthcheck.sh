#!/bin/sh

etcdctl endpoint health --user "gms:gmsdb:etcd:gms:packager-tungsten-sort"

if [ ?$ -ne 0 ]; then
    echo "GMS system configuration etcd server not running..."
    exit 1
fi

echo "GMS system configuration is available..."
exit 0

#!/bin/bash

# This script is run to set up etcd

set -eu

if [ -z "${GMS_ETCD_ROOT_PASSWORD}" -o -z "${GMS_ETCD_ADMIN_PASSWORD}" -o -z "${GMS_ETCD_PASSWORD}" ]; then
    echo Unable to apply GMS etcd configuration - environment variables must be defined: GMS_ETCD_ROOT_PASSWORD, GMS_ETCD_ADMIN_PASSWORD, GMS_ETCD_PASSWORD
    exit 1
fi

#-- Start etcd temporarily for configuration and loading
etcd &
etcdpid=$!

#-- Wait for etcd to fully initialize
until etcdctl endpoint health; do
    sleep 1
done

#-- Add 'root' user and enable authentication
etcdctl user add "root:${GMS_ETCD_ROOT_PASSWORD}"
etcdctl auth enable

#-- Setup 'read-everything' and 'readwrite-everything' roles
etcdctl role add read-everything --user "root:${GMS_ETCD_ROOT_PASSWORD}"
etcdctl role add readwrite-everything --user "root:${GMS_ETCD_ROOT_PASSWORD}"
etcdctl role grant-permission --prefix read-everything read '' --user "root:${GMS_ETCD_ROOT_PASSWORD}"
etcdctl role grant-permission --prefix readwrite-everything readwrite '' --user "root:${GMS_ETCD_ROOT_PASSWORD}"

#-- Setup 'gmsadmin' user 
etcdctl user add "gmsadmin:${GMS_ETCD_ADMIN_PASSWORD}" --user "root:${GMS_ETCD_ROOT_PASSWORD}"
etcdctl user grant-role gmsadmin readwrite-everything --user "root:${GMS_ETCD_ROOT_PASSWORD}"

#-- Load configuration as 'gmsadmin'
gms-config --username gmsadmin --password "${GMS_ETCD_ADMIN_PASSWORD}" --endpoints localhost load /setup/config/system/gms-system-configuration.properties

#-- Setup 'gms' user
etcdctl --dial-timeout=6s user add "gms:${GMS_ETCD_PASSWORD}" --user "root:${GMS_ETCD_ROOT_PASSWORD}"
etcdctl --dial-timeout=6s user grant-role gms read-everything --user "root:${GMS_ETCD_ROOT_PASSWORD}"
sleep 1

#-- Stop the now-configured etcd
kill ${etcdpid}

#
#  Dockerfile to make a configured etcd image for GMS.
#
ARG DOCKER_REGISTRY
ARG DOCKER_IMAGE_TAG
ARG PROJECT_NAME

FROM $DOCKER_REGISTRY/${PROJECT_NAME}/python:$DOCKER_IMAGE_TAG

# Install etcd
RUN set -ex && \
    yum -y install etcd && \
    conda install -n base -y grpcio==1.12.1

# Configure a single-node etcd server.  ETCD_NAME is definied
# in a separate ENV statement so that it can be interpolated
# later when setting ETCD_INITIAL_CLUSTER.  
ENV ETCD_NAME=etcd1
ENV USER_NAME=etcd \
    ETCDCTL_API=3 \
    ETCD_DATA_DIR=/var/lib/etcd \
    ETCD_INITIAL_ADVERTISE_PEER_URLS=http://127.0.0.1:2380 \
    ETCD_LISTEN_PEER_URLS=http://0.0.0.0:2380 \
    ETCD_ADVERTISE_CLIENT_URLS=http://127.0.0.1:2379 \
    ETCD_LISTEN_CLIENT_URLS=http://0.0.0.0:2379 \
    ETCD_INITIAL_CLUSTER="${ETCD_NAME}=http://127.0.0.1:2380"

# Setup group-write permissions on data directory and /etc/passwd
RUN set -ex && \
    mkdir -p ${ETCD_DATA_DIR} && \
    chgrp -R 0 ${ETCD_DATA_DIR} /etc/passwd && \
    chmod -R g=u ${ETCD_DATA_DIR} /etc/passwd
WORKDIR /

# Install gms-config Python interface
COPY docker-build/gms-config /setup/gms-config
RUN set -ex && \
    pip install /setup/gms-config

# Copy in the the system configuration files
COPY docker-build/system/* /setup/config/system/

ARG GMS_ETCD_PASSWORD
ARG GMS_ETCD_ADMIN_PASSWORD
ARG GMS_ETCD_ROOT_PASSWORD
ENV GMS_ETCD_PASSWORD=${GMS_ETCD_PASSWORD} \
    GMS_ETCD_ADMIN_PASSWORD=${GMS_ETCD_ADMIN_PASSWORD} \
    GMS_ETCD_ROOT_PASSWORD=${GMS_ETCD_ROOT_PASSWORD}

# Copy in the the etcd setup script and set up etcd
COPY etcd-setup.sh /usr/local/bin/etcd-setup.sh
RUN /usr/local/bin/etcd-setup.sh

# Copy in the the health-check.sh script
COPY docker-etcd-healthcheck.sh /usr/local/bin

HEALTHCHECK --interval=30s --timeout=30s --retries=5 CMD docker-etcd-healthcheck.sh || exit 1

ARG CI_COMMIT_REF_NAME=NA
ARG CI_COMMIT_SHA=NA
ARG CI_BUILD_DATETIME=NA

ENV CI_COMMIT_REF_NAME=${CI_COMMIT_REF_NAME} \
    CI_COMMIT_SHA=${CI_COMMIT_SHA} \
    CI_BUILD_DATETIME=${CI_BUILD_DATETIME}
    
LABEL gms.ci.commit.ref.name=${CI_COMMIT_REF_NAME} \
      gms.ci.commit.sha=${CI_COMMIT_SHA} \
      gms.ci.build.datetime=${CI_BUILD_DATETIME}

CMD ["etcd"]

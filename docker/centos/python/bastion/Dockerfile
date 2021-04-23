#
#  Dockerfile to make a 'bastion' image for GMS.
#
#  This is a container usable for system maintenance and diagnostics.
#
#  docker exec -it [ bastion image id ] /bin/bash
#

ARG DOCKER_REGISTRY
ARG DOCKER_IMAGE_TAG
ARG PROJECT_NAME

FROM ${DOCKER_REGISTRY}/${PROJECT_NAME}/python:${DOCKER_IMAGE_TAG}

ARG LIBRDKAFKA_VERSION=1.3.0
ARG KAFKACAT_VERSION=1.5.0
ARG GITHUB_URL=https://github.com

WORKDIR /bastion

# download kafkacat
ADD ${GITHUB_URL}/edenhill/librdkafka/archive/v${LIBRDKAFKA_VERSION}.tar.gz /bastion/v${LIBRDKAFKA_VERSION}.tar.gz
ADD ${GITHUB_URL}/edenhill/kafkacat/archive/${KAFKACAT_VERSION}.tar.gz /bastion/${KAFKACAT_VERSION}.tar.gz

# build
RUN set -ex && \
    yum -y install gcc-c++ cmake openssl postgresql && \
    \
    # untar
    tar -xzf v${LIBRDKAFKA_VERSION}.tar.gz && \
    tar -xzf ${KAFKACAT_VERSION}.tar.gz && \
    \
    # build librdkafka
    cd /bastion/librdkafka-${LIBRDKAFKA_VERSION} && \
    ./configure && \
    make && \
    make install && \
    \
    # build kafkacat
    cd /bastion/kafkacat-${KAFKACAT_VERSION} && \
    LDFLAGS="-pthread -lm -ldl" ./configure --enable-static --enable-json && \
    make && \
    make install && \
    \
    # cleanup
    cd /bastion && \
    rm -rf v${LIBRDKAFKA_VERSION}.tar.gz librdkafka-${LIBRDKAFKA_VERSION} && \
    rm -rf ${KAFKACAT_VERSION}.tar.gz kafkacat-${KAFKACAT_VERSION} && \
    yum -y remove gcc-c++ && \
    yum clean all

# If we are running, we are healthy
HEALTHCHECK CMD ps -o args -p 1 | grep sleep

ARG CI_COMMIT_REF_NAME=NA
ARG CI_COMMIT_SHA=NA
ARG CI_BUILD_DATETIME=NA

ENV CI_COMMIT_REF_NAME=${CI_COMMIT_REF_NAME} \
    CI_COMMIT_SHA=${CI_COMMIT_SHA} \
    CI_BUILD_DATETIME=${CI_BUILD_DATETIME}

LABEL gms.ci.commit.ref.name=${CI_COMMIT_REF_NAME} \
      gms.ci.commit.sha=${CI_COMMIT_SHA} \
      gms.ci.build.datetime=${CI_BUILD_DATETIME}

# Run in the background forever.
# System maintainers should `docker exec -it [ bastion image id ] /bin/bash` to access
CMD ["/bin/sleep", "infinity"]

# Docker build arguments
ARG DOCKER_REGISTRY
ARG DOCKER_IMAGE_TAG
FROM $DOCKER_REGISTRY/gms-common/java:$DOCKER_IMAGE_TAG

# Copy tars, untar them
WORKDIR /data-receiver/

COPY cd11-connman-*.tar ./

RUN set -ex && \
    mkdir cd11-connman && \
    mkdir -p shared-volume/logs && \
    chmod -R 777 shared-volume/ && \
    tar -xf cd11-connman-*.tar -C cd11-connman --strip-components 1 && \
    rm -f cd11-connman-*.tar && \
    mkdir -p shared-volume/logs && \
    chmod -R 777 shared-volume/

ARG CI_COMMIT_REF_NAME=NA
ARG CI_COMMIT_SHA=NA
ARG CI_BUILD_DATETIME=NA

ENV CI_COMMIT_REF_NAME=${CI_COMMIT_REF_NAME} \
    CI_COMMIT_SHA=${CI_COMMIT_SHA} \
    CI_BUILD_DATETIME=${CI_BUILD_DATETIME}
    
LABEL gms.ci.commit.ref.name=${CI_COMMIT_REF_NAME} \
      gms.ci.commit.sha=${CI_COMMIT_SHA} \
      gms.ci.build.datetime=${CI_BUILD_DATETIME}

CMD cd11-connman/bin/cd11-connman

HEALTHCHECK --interval=30s --retries=10 \
 CMD curl -f localhost:8080/alive || exit 1

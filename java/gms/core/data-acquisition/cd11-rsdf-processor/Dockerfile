# Run CD1.1 RSDF processing topology

ARG DOCKER_REGISTRY
ARG DOCKER_IMAGE_TAG
FROM $DOCKER_REGISTRY/gms-common/java:$DOCKER_IMAGE_TAG

# Copy tars, untar them
WORKDIR /cd11-rsdf-processor/

COPY ./cd11-rsdf-processor-*.tar * ./

RUN set -ex && \
    mkdir cd11-rsdf-processor && \
    tar -xf cd11-rsdf-processor-*.tar --strip 1 -C cd11-rsdf-processor && \
    rm -f cd11-rsdf-processor-*.tar && \
    chgrp -R 0 /cd11-rsdf-processor && \
    chmod -R g=u /cd11-rsdf-processor && \
    chmod -R g+s /cd11-rsdf-processor

ARG CI_COMMIT_REF_NAME=NA
ARG CI_COMMIT_SHA=NA
ARG CI_BUILD_DATETIME=NA

ENV CI_COMMIT_REF_NAME=${CI_COMMIT_REF_NAME} \
    CI_COMMIT_SHA=${CI_COMMIT_SHA} \
    CI_BUILD_DATETIME=${CI_BUILD_DATETIME}
    
LABEL gms.ci.commit.ref.name=${CI_COMMIT_REF_NAME} \
      gms.ci.commit.sha=${CI_COMMIT_SHA} \
      gms.ci.build.datetime=${CI_BUILD_DATETIME}

# Start topology
CMD cd11-rsdf-processor/bin/cd11-rsdf-processor

HEALTHCHECK --interval=30s --retries=10 \
 CMD curl -f localhost:8080/alive || exit 1


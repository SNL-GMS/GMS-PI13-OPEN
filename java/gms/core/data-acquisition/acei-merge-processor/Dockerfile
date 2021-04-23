# Run CD1.1 RSDF processing topology

ARG DOCKER_REGISTRY
ARG DOCKER_IMAGE_TAG
FROM $DOCKER_REGISTRY/gms-common/java:$DOCKER_IMAGE_TAG

# Copy tars, untar them
WORKDIR /acei-merge-processor/

COPY ./acei-merge-processor-*.tar * ./

RUN set -ex && \
    mkdir acei-merge-processor && \
    tar -xf acei-merge-processor-*.tar --strip 1 -C acei-merge-processor && \
    rm -f acei-merge-processor-*.tar && \
    chgrp -R 0 /acei-merge-processor && \
    chmod -R g=u /acei-merge-processor && \
    chmod -R g+s /acei-merge-processor

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
CMD acei-merge-processor/bin/acei-merge-processor

HEALTHCHECK --interval=30s --retries=10 \
 CMD curl -f localhost:8080/alive || exit 1

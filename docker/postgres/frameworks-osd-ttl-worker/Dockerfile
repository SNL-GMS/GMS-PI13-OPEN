#
#  Dockerfile to make a 'frameworks-osd-ttl-worker' image for GMS.
#

ARG DOCKER_REGISTRY
ARG DOCKER_IMAGE_TAG
ARG PROJECT_NAME

FROM ${DOCKER_REGISTRY}/${PROJECT_NAME}/postgres:${DOCKER_IMAGE_TAG}

WORKDIR /ttl

COPY trigger-stored-procedure-periodically /ttl

# If we are running, we are healthy
HEALTHCHECK --interval=30s CMD /bin/true

ARG CI_COMMIT_REF_NAME=NA
ARG CI_COMMIT_SHA=NA
ARG CI_BUILD_DATETIME=NA

ENV CI_COMMIT_REF_NAME=${CI_COMMIT_REF_NAME} \
    CI_COMMIT_SHA=${CI_COMMIT_SHA} \
    CI_BUILD_DATETIME=${CI_BUILD_DATETIME}

LABEL gms.ci.commit.ref.name=${CI_COMMIT_REF_NAME} \
      gms.ci.commit.sha=${CI_COMMIT_SHA} \
      gms.ci.build.datetime=${CI_BUILD_DATETIME}

CMD ["/ttl/trigger-stored-procedure-periodically"]

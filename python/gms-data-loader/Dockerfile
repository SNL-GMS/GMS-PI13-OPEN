ARG DOCKER_REGISTRY
ARG DOCKER_IMAGE_TAG
FROM $DOCKER_REGISTRY/gms-common/python:$DOCKER_IMAGE_TAG

ENV PYTHONUNBUFFERED=1

WORKDIR /gms-data-loader
COPY . /gms-data-loader

RUN set -ex && \
    python3 setup.py install

ARG CI_COMMIT_REF_NAME=NA
ARG CI_COMMIT_SHA=NA
ARG CI_BUILD_DATETIME=NA
ARG CI_VERSION=NA

ENV CI_COMMIT_REF_NAME=${CI_COMMIT_REF_NAME} \
    CI_COMMIT_SHA=${CI_COMMIT_SHA} \
    CI_BUILD_DATETIME=${CI_BUILD_DATETIME} \
    CI_VERSION=${CI_VERSION}

LABEL gms.ci.commit.ref.name=${CI_COMMIT_REF_NAME} \
      gms.ci.commit.sha=${CI_COMMIT_SHA} \
      gms.ci.build.datetime=${CI_BUILD_DATETIME} \
      gms.ci.version=${CI_VERSION}

ENTRYPOINT ["gms-data-loader.py"]

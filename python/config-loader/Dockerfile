ARG DOCKER_REGISTRY
ARG DOCKER_IMAGE_TAG
ARG PROJECT_NAME

FROM ${DOCKER_REGISTRY}/${PROJECT_NAME}/python:${DOCKER_IMAGE_TAG}

EXPOSE 8080

WORKDIR /config-loader
COPY environment.yml /install/environment.yml

SHELL [ "/bin/bash", "-l", "-c" ]
RUN set -ex && conda env update -n=base -f=/install/environment.yml && conda activate base

COPY ./app /config-loader/app
COPY ./gunicorn-config.py /config-loader/gunicorn-config.py
COPY ./_python/gms-utils /install/gms-utils  
COPY ./_python/gms-data-loader /install/gms-data-loader
COPY ./_config /base

RUN set -ex && \
    mkdir /override && \
    cd /install/gms-utils && python3 setup.py install && \
    cd /install/gms-data-loader && python3 setup.py install

ARG CI_COMMIT_REF_NAME=NA
ARG CI_COMMIT_SHA=NA
ARG CI_BUILD_DATETIME=NA

ENV CI_COMMIT_REF_NAME=${CI_COMMIT_REF_NAME} \
    CI_COMMIT_SHA=${CI_COMMIT_SHA} \
    CI_BUILD_DATETIME=${CI_BUILD_DATETIME}

LABEL gms.ci.commit.ref.name=${CI_COMMIT_REF_NAME} \
      gms.ci.commit.sha=${CI_COMMIT_SHA} \
      gms.ci.build.datetime=${CI_BUILD_DATETIME}

HEALTHCHECK --interval=30s CMD curl -f localhost:8080/alive || exit 1

ENTRYPOINT ["gunicorn", "--config=gunicorn-config.py", "app"]

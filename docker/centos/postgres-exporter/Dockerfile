ARG DOCKER_REGISTRY
ARG DOCKER_IMAGE_TAG
ARG PROJECT_NAME

FROM ${DOCKER_REGISTRY}/${PROJECT_NAME}/centos:${DOCKER_IMAGE_TAG} as builder

ARG PG_EXPORTER_VERSION=0.8.0
ARG GITHUB_URL=https://github.com

WORKDIR /postgres_exporter

ADD ${GITHUB_URL}/wrouesnel/postgres_exporter/releases/download/v${PG_EXPORTER_VERSION}/postgres_exporter_v${PG_EXPORTER_VERSION}_linux-amd64.tar.gz /postgres_exporter/postgres_exporter_v${PG_EXPORTER_VERSION}_linux-amd64.tar.gz

RUN set -ex && \
    tar -xvf postgres_exporter_v${PG_EXPORTER_VERSION}_linux-amd64.tar.gz

FROM ${DOCKER_REGISTRY}/${PROJECT_NAME}/centos:${DOCKER_IMAGE_TAG}

ARG PG_EXPORTER_VERSION=0.8.0
ENV PG_EXPORTER_VERSION=${PG_EXPORTER_VERSION}

WORKDIR /postgres_exporter

COPY --from=builder /postgres_exporter/postgres_exporter_v${PG_EXPORTER_VERSION}_linux-amd64/postgres_exporter /bin

RUN useradd -r postgres_exporter

EXPOSE 9187

USER postgres_exporter

ARG CI_COMMIT_REF_NAME=NA
ARG CI_COMMIT_SHA=NA
ARG CI_BUILD_DATETIME=NA

ENV CI_COMMIT_REF_NAME=${CI_COMMIT_REF_NAME} \
    CI_COMMIT_SHA=${CI_COMMIT_SHA} \
    CI_BUILD_DATETIME=${CI_BUILD_DATETIME}

LABEL gms.ci.commit.ref.name=${CI_COMMIT_REF_NAME} \
      gms.ci.commit.sha=${CI_COMMIT_SHA} \
      gms.ci.build.datetime=${CI_BUILD_DATETIME}

HEALTHCHECK --interval=30s \
  CMD curl -f localhost:9187 \
    || exit 1

ENTRYPOINT [ "/bin/postgres_exporter" ]

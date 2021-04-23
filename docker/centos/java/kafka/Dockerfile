ARG DOCKER_REGISTRY
ARG DOCKER_IMAGE_TAG
ARG PROJECT_NAME

FROM ${DOCKER_REGISTRY}/${PROJECT_NAME}/java:${DOCKER_IMAGE_TAG} as builder

ARG KAFKA_VERSION=2.2.0
ARG SCALA_VERSION=2.12
ARG APACHE_URL=https://archive.apache.org

# download kafka and jmx_exporter
ADD ${APACHE_URL}/dist/kafka/${KAFKA_VERSION}/kafka_${SCALA_VERSION}-${KAFKA_VERSION}.tgz /tmp/kafka_${SCALA_VERSION}-${KAFKA_VERSION}.tgz

RUN tar xfz /tmp/kafka_${SCALA_VERSION}-${KAFKA_VERSION}.tgz -C /opt

ARG DOCKER_REGISTRY
ARG DOCKER_IMAGE_TAG
ARG PROJECT_NAME

FROM ${DOCKER_REGISTRY}/${PROJECT_NAME}/java:${DOCKER_IMAGE_TAG}

ARG KAFKA_VERSION=2.2.0
ARG SCALA_VERSION=2.12
ARG JMX_EXPORTER_VERSION=0.12.0
ARG METRICS_PORT=7071
ARG ENABLE_METRICS="off"
ARG APACHE_URL=https://archive.apache.org
ARG MAVEN_URL=https://repo1.maven.org

ENV KAFKA_VERSION=${KAFKA_VERSION} \
    SCALA_VERSION=${SCALA_VERSION} \
    JMX_EXPORTER_VERSION=${JMX_EXPORTER_VERSION} \
    KAFKA_HOME=/opt/kafka \
    METRICS_PORT=${METRICS_PORT} \
    ENABLE_METRICS=${ENABLE_METRICS}
ENV PATH=${PATH}:${KAFKA_HOME}/bin

COPY src/start-kafka.sh src/broker-list.sh src/create-topics.sh src/versions.sh src/kafka-health-check.sh /usr/bin/
COPY --from=builder /opt/kafka_${SCALA_VERSION}-${KAFKA_VERSION} /opt/kafka_${SCALA_VERSION}-${KAFKA_VERSION} 

RUN set -ex && \
    # uses epel from centos image
    # install deps
    yum install -y net-tools docker-client && \
    # setup kafka
    ln -s /opt/kafka_${SCALA_VERSION}-${KAFKA_VERSION} ${KAFKA_HOME} && \
    # setup jmx_exporter
    mkdir -p /opt/kafka/prometheus && \
    ln -s /opt/kafka/prometheus/jmx_prometheus_javaagent-$JMX_EXPORTER_VERSION.jar /opt/kafka/prometheus/jmx_prometheus_javaagent.jar && \
    yum clean all

ADD ${MAVEN_URL}/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/${JMX_EXPORTER_VERSION}/jmx_prometheus_javaagent-${JMX_EXPORTER_VERSION}.jar /opt/kafka/prometheus/jmx_prometheus_javaagent-${JMX_EXPORTER_VERSION}.jar

COPY src/kafka.yml /opt/kafka/prometheus

VOLUME ["/kafka"]

HEALTHCHECK --interval=30s --start-period=90s --retries=10 CMD kafka-health-check.sh

ARG CI_COMMIT_REF_NAME=NA
ARG CI_COMMIT_SHA=NA
ARG CI_BUILD_DATETIME=NA

ENV CI_COMMIT_REF_NAME=${CI_COMMIT_REF_NAME} \
    CI_COMMIT_SHA=${CI_COMMIT_SHA} \
    CI_BUILD_DATETIME=${CI_BUILD_DATETIME}

LABEL gms.ci.commit.ref.name=${CI_COMMIT_REF_NAME} \
      gms.ci.commit.sha=${CI_COMMIT_SHA} \
      gms.ci.build.datetime=${CI_BUILD_DATETIME}

# Use "exec" form so that it runs as PID 1 (useful for graceful shutdown)
CMD ["start-kafka.sh"]

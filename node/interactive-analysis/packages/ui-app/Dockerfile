#
# Dockerfile for running an NGINX instance serving up the GMS analyst user interface.
#

# Start with the project NGINX base image
ARG DOCKER_REGISTRY
ARG DOCKER_IMAGE_TAG
ARG PROJECT_NAME

FROM ${DOCKER_REGISTRY}/${PROJECT_NAME}/nginx:${DOCKER_IMAGE_TAG}

# Install the gettext package for the envsubst command used in the ENTRYPOINT
USER root:root

# Copy files into the container
COPY ./nginx/ /etc/nginx
COPY ./docker-entrypoint.sh /docker-entrypoint.sh

# Create a directory for the interactive analysis UI
# and copy the UI files into the container
WORKDIR /opt/interactive-analysis-ui/

COPY ./dist/ ./

# Since we're running as non-root, change the owner of places where NGINX writes
RUN set -ex && \
    chown nginx:root /opt/interactive-analysis-ui -R && chmod -R g+w /opt/interactive-analysis-ui && \
    chown nginx:root /etc/nginx/ -R && chmod -R g+w /etc/nginx/ && \
    chown nginx:root /var/cache/nginx/ -R && chmod -R g+w /var/cache/nginx/ && \
    chown nginx:root /var/log/nginx/ -R && chmod -R g+w /var/log/nginx/ && \
    chown nginx:root /docker-entrypoint.sh && chmod -R g+w /docker-entrypoint.sh

# Set the user to the nginx user
USER nginx:root

# Expose NGINX ports
EXPOSE 8080 8443

ARG CI_COMMIT_REF_NAME=NA
ARG CI_COMMIT_SHA=NA
ARG CI_BUILD_DATETIME=NA

ENV CI_COMMIT_REF_NAME=${CI_COMMIT_REF_NAME} \
    CI_COMMIT_SHA=${CI_COMMIT_SHA} \
    CI_BUILD_DATETIME=${CI_BUILD_DATETIME}
    
LABEL gms.ci.commit.ref.name=${CI_COMMIT_REF_NAME} \
      gms.ci.commit.sha=${CI_COMMIT_SHA} \
      gms.ci.build.datetime=${CI_BUILD_DATETIME}

# Set the run command to:
# 1. Create the ngninx conf file based on a template file and environment variables
# 2. Run nginx in the foreground ('deamon off')
ENTRYPOINT ["/bin/bash", "-c", "/docker-entrypoint.sh"]

HEALTHCHECK --interval=30s --retries=10 \
 CMD curl -f localhost:8080/alive || exit 1

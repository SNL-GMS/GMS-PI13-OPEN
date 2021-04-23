ARG DOCKER_REGISTRY
ARG DOCKER_IMAGE_TAG
ARG PROJECT_NAME

FROM ${DOCKER_REGISTRY}/${PROJECT_NAME}/centos:${DOCKER_IMAGE_TAG}

ARG NGINX_YUM_URL=http://nginx.org

# Configure an nginx repo and install
ADD src/nginx.repo /etc/yum.repos.d/
#ADD src/RPM-GPG-KEY-EPEL-7 /etc/pki/rpm-gpg/RPM-GPG-KEY-EPEL-7
ADD src/nginx_signing.key /etc/pki/rpm-gpg/nginx_signing.key

RUN set -ex && \
    sed -i -e "s~#NGINX_YUM_URL#~$NGINX_YUM_URL~g" /etc/yum.repos.d/nginx.repo && \
    yum update -y && \
    yum install -y nginx && \
    yum clean all

# Since we're running as non-root, change the owner of places
# where nginx writes
RUN chown nginx:nginx /var/cache/nginx/ -R && \
    chown nginx:nginx /var/log/nginx/ -R

# Expose NGINX ports
EXPOSE 8080 8443

USER nginx

ENTRYPOINT ["nginx", "-g", "daemon off;"]

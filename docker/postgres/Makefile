NAME = postgres

BUILD_ARGS = --build-arg 'FROM_IMAGE=$${CI_THIRD_PARTY_DOCKER_REGISTRY}/postgres:12.1'

SUBDIRS := frameworks-osd-ttl-worker

include $(shell CI-MAKE-CONFIG-PATH)/Makefile.docker

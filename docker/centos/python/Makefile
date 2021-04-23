NAME = python

BUILD_ARGS = --build-arg 'MINICONDA_URL=$${MINICONDA_URL}' \
             --build-arg 'PYPI_URL=$${PYPI_URL}'

SUBDIRS := bastion

CLEAN = rm -rf Dockerfile

include $(shell CI-MAKE-CONFIG-PATH)/Makefile.docker

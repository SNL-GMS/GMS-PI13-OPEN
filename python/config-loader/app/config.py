import os
import logging
from typing import List, Type

basedir = os.path.abspath(os.path.dirname(__file__))

class BaseConfig:
    CONFIG_NAME = "base"
    LOG_LEVEL = "INFO"
    DEBUG = False
    EXECUTOR_TYPE = 'thread'
    EXECUTOR_PROPAGATE_EXCEPTIONS = True
    PROCESSING_CONFIG_SERVICE_NAME = "frameworks-configuration-service"
    OSD_SERVICE_NAME = "frameworks-osd-service"
    BASE_CONFIG_PATH = "/base"
    OVERRIDE_CONFIG_PATH = "/override"
    SERVICE_WAIT_TIMEOUT = 60
    SERVICE_CHECK_INTERVAL = 5
    TARFILE_NAME_FULLPATH = '/tmp/tardata.tar.gz'

class DevelopmentConfig(BaseConfig):
    CONFIG_NAME = "dev"
    DEBUG = True
    TESTING = False

class TestingConfig(BaseConfig):
    CONFIG_NAME = "test"
    DEBUG = True
    TESTING = True

class ProductionConfig(BaseConfig):
    CONFIG_NAME = "prod"
    DEBUG = False
    TESTING = False

EXPORT_CONFIGS: List[Type[BaseConfig]] = [
    DevelopmentConfig,
    TestingConfig,
    ProductionConfig,
]

config_by_name = {cfg.CONFIG_NAME: cfg for cfg in EXPORT_CONFIGS}

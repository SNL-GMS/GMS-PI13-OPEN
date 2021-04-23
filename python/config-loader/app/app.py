#!/usr/bin/env python3

# --------------------------------------------------------------------
#  config-loader
#
#  The config-loader service...
# --------------------------------------------------------------------

import os
import sys
import logging
import threading
import time
import tarfile

from flask import Flask, jsonify
from flask import current_app
from flask import request
from flask_executor import Executor

logger = logging.getLogger(__package__)

def create_app(env=None):
    from . import app_state
    from . import executor
    from . import config_by_name
    from . import State
    from . import loader
    from . import initiate_load
    from . import initiate_reload

    app = Flask(__name__)
    app.config.from_object(config_by_name[env or "test"])

    logging.basicConfig(format='[%(asctime)s] [%(process)s] [%(levelname)s] %(message)s', level=app.config['LOG_LEVEL'])
    
    executor.init_app(app)
    
    with app.app_context():

        # Environment option to automatically load default configuration on startup for testing
        if environment_bool('GMS_CONFIG_AUTOLOAD_DEFAULTS'):
            with app.test_request_context():
                initiate_load('load_results')
        
        @app.route("/alive")
        def alive():
            return jsonify("alive"), 200  # If we are running, then we report that we are alive.

        @app.route("/initialized")
        def initialized():
            if app_state.get_state() == 'loaded':
                return jsonify("loaded")
            else:
                return jsonify("not loaded"), 500 # How will this work with kubernetes initcontainers?
            
        @app.route("/load", methods=['POST'])
        def load():
            f = request.files.get('files')
            if f:
                f.save(current_app.config["TARFILE_NAME_FULLPATH"])
                tar = tarfile.open(current_app.config["TARFILE_NAME_FULLPATH"])
                tar.extractall(current_app.config["OVERRIDE_CONFIG_PATH"])
                tar.close()

                # Remove the tar file
                if os.path.exists(current_app.config["TARFILE_NAME_FULLPATH"]):
                    os.remove(current_app.config["TARFILE_NAME_FULLPATH"])                

            return initiate_load('load_results')
            
        @app.route("/reload", methods=['POST'])
        def reload():
            f = request.files['files']
            if f:
                f.save(current_app.config["TARFILE_NAME_FULLPATH"])
                tar = tarfile.open(current_app.config["TARFILE_NAME_FULLPATH"])
                tar.extractall(current_app.config["OVERRIDE_CONFIG_PATH"])
                tar.close()

                # Remove the tar file
                if os.path.exists(current_app.config["TARFILE_NAME_FULLPATH"]):
                    os.remove(current_app.config["TARFILE_NAME_FULLPATH"])                

            return initiate_reload('load_results')
    
        @app.route("/result")
        def result():
            if not executor.futures.done('load_results'):
                return jsonify({'status': executor.futures._state('load_results')})
            future = executor.futures.pop('load_results')
            success, log_output = future.result()
            return jsonify({'status': 'FINISHED', 'successful': success, 'result': log_output})

        @app.route("/service-internal-state")
        def service_state():
            return app_state.as_dict()

        @app.errorhandler(404)
        def not_found_error(error):
            return '404 error', 404

    return app

def environment_bool(name):
    value = os.environ.get(name, 'false').lower()
    return not (value == '0' or value == 'false')  # Consider *anything* except '0' or 'false' to be True



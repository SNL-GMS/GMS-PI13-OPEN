#!/usr/local/bin python3

# --------------------------------------------------------------------
#  processing-config-loader.py - GMS Processing Configuration Loader
#
#  The processing-config-loader command-line program is used to load
#  processing configuration information into the configuration database
#  for deployments of the GMS (Geophysical Monitoring System) system.
# --------------------------------------------------------------------

import datetime
import json
import os
import sys
import time
from os import listdir

import requests
import yaml

PROCESSING_CONFIGURATION_ROOT = sys.argv[1]
API_ENDPOINT = sys.argv[2] + "/processing-cfg/put-all"

def main():
    print("process-config-loader started")
    print("processing_configuration_root: " + PROCESSING_CONFIGURATION_ROOT)
    print("API_ENDPOINT: " + API_ENDPOINT)

    check_help()

    config_dict = get_config_files()
    json_configuration_dict = {}
    cfg_list = create_configurations(config_dict, json_configuration_dict)
    post_cfgs(cfg_list)
    time.sleep(10)
    print("process-config-loader complete")
    exit(0)

class Configuration:

    def __init__(self, name):
        self.name = name
        self.configurationOptions = []  #NOSONAR - Jackson not unmarshalling this correctly if using snake_case
        self.changeTime = datetime.datetime.now().timestamp() #NOSONAR - Jackson not unmarshalling this correctly if using snake_case


    def get_name(self):
        return self.name

    def add_configuration_option(self, configuration_option_json):
        configuration_option = json.loads(configuration_option_json)
        if type(configuration_option) == list:
            for configuration in configuration_option:
                self.configurationOptions.append(configuration)
        else:
            self.configurationOptions.append(configuration_option) #NOSONAR - Jackson not unmarshalling this correctly if using snake_case



#
# display help to user
#
def check_help():
    if (PROCESSING_CONFIGURATION_ROOT == '--help'):
        description = """
description:
  The process-config-loader command-line program is used add/update
  processing configurations to be used by services throught the GMS system.
 
  Users may optionally provide the location of the processing configurations to be uploaded and the location of the processing-config-server
    This is used when mounting your own configuration after the system has been bootstrapped
  
  ./processing-config-loader [path-to-config-files] [location-of-processing-config-server]
  
    NOTE: the order of the arguments matters and the 1st argument is required to pass in the 2nd   
"""
        print(description)
        exit(0)

#
# traverses files located in processing-configuration-root to get all the relative paths to the yml files
#
def get_config_files():
    print("loading configuration files from processingConfigurationRoot directory: " + PROCESSING_CONFIGURATION_ROOT)
    config_dict = {}

    is_directory = os.path.isdir(PROCESSING_CONFIGURATION_ROOT)
    if not is_directory:
        print("Processing-configuration-root must be a directory")
        exit(-1)

    for dir in listdir(PROCESSING_CONFIGURATION_ROOT):
       config_dict[dir] = os.listdir(PROCESSING_CONFIGURATION_ROOT + "/" + dir)

    print("Loaded (" + str(len(config_dict)) + ") configurations")
    return config_dict
#
# Creates Configuration Objects from yml files located in the fileSet collection
# adds json files to another dictionary for later processing
#
def create_configurations(config_dict, json_configuration_dict):
    cfg_list = []
    print("----- Processing Configuration files started -----")

    for dir_name, files in config_dict.items():
        print("Processing configuration options from Configuration dir: " + dir_name)
        cfg = Configuration(dir_name)
        for config_file in files:
            cfg_option_filename = PROCESSING_CONFIGURATION_ROOT + "/" + dir_name + "/" + config_file;
            print("Loading configuration option from file: " + cfg_option_filename)

            with open(cfg_option_filename) as file:
                if cfg_option_filename.endswith(('.json', '.yaml', '.yml')):
                    if cfg_option_filename.endswith(".json"):
                        data = json.dumps(json.load(file))
                    else:
                        data = json.dumps(yaml.full_load(file))
                    cfg.add_configuration_option(data)
        cfg_list.append(cfg)

    print("----- Processing Configuration files complete -----")
    return cfg_list

#
# create json aray from Configuration objects and post to the processing-configuration-service
#
def post_cfgs(cfg_list):
    json_list = []
    print("Generating json to POST to server")
    for cfg in cfg_list:
        json_list.append(json.loads(json.dumps(cfg.__dict__)))
    headers = {'Content-Type': 'application/json', 'Accept': 'application/json'}
    print("Posting Configurations to: " + API_ENDPOINT)
    r = requests.post(url=API_ENDPOINT, json=json_list, headers=headers)
    print("Server response: " + str(r))

if __name__== "__main__" :
    main()

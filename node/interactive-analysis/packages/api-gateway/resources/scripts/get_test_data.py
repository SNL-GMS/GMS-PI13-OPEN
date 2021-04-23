'''
Pulls latest test data from docker image

Running the script:

python build_unit_test_data.py --testDataHome <testDataHome>

Parameters: 

--testDataHome <testDataHome> - Top level directory for Test Data Sets
'''

import os
import time
import argparse

CI_DOCKER_REGISTRY = os.getenv('CI_DOCKER_REGISTRY')

parser = argparse.ArgumentParser()
parser.add_argument('--testDataHome', dest='testDataHome', help='Top level directory for Test Data Sets')
parser.add_argument('--repoHome', dest='repoHome', help='Top level directory for gms_common')
parser.add_argument('--dockerVersion', dest='dockerVersion', help='Version of the test data to grab: Latest will be used if arg is not provided')

args = parser.parse_args()

# Extract command-line arguments
testDataHome = args.testDataHome
repoHome = args.repoHome
dockerVersion = args.dockerVersion

if (not (testDataHome) or not (repoHome)):
    parser.print_help()
    exit(0)

if not (dockerVersion):
    dockerVersion = 'latest'

print(dockerVersion)

stdsLocationInRepo = '/node/interactive-analysis/packages/api-gateway/resources/test_data/unit-test-data/Standard_Test_Data'

print('Getting Docker Image: ' + dockerVersion)
os.system('docker pull ' + CI_DOCKER_REGISTRY + '/standard-test-data-set-loader:' + dockerVersion)
print('Starting Docker Image in Background')
os.system('docker run -i --name stds ' + CI_DOCKER_REGISTRY + '/standard-test-data-set-loader:' + dockerVersion + ' bash &')
time.sleep(3)
print('Copying files from Docker Image')
os.system('docker cp stds:/standard-test-data-set ' + testDataHome)
print('Stopping docker container')
os.system('docker stop stds')
os.system('docker rm stds')
print('Cleaning up files')
os.system('rm -rf ' + testDataHome + '/standard-test-data-set/scripts')
os.system('rm -rf ' + testDataHome + '/standard-test-data-set/waveform-loader')
os.system('rm -rf ' + testDataHome + '/Standard_Test_Data')
os.system('mv ' + testDataHome + '/standard-test-data-set ' + testDataHome + '/Standard_Test_Data')
print('Copying stds files to repo for unit test data')
os.system('cp -r ' + testDataHome + '/Standard_Test_Data/gms_test_data_set/ ' + repoHome + stdsLocationInRepo + '/gms_test_data_set/')
os.system('rm -rf ' + repoHome + stdsLocationInRepo + '/gms_test_data_set/responses')
fileName = os.listdir(testDataHome + '/Standard_Test_Data/feature-prediction')[0]
os.system('cp -r ' + testDataHome + '/Standard_Test_Data/feature-prediction/' + fileName + ' ' + repoHome + stdsLocationInRepo + '/feature-prediction')
fileName = list(filter(lambda f: f.split('.')[-1] == 'json', os.listdir(testDataHome + '/Standard_Test_Data/FkSpectra/')))[0]
os.system('cp ' + testDataHome + '/Standard_Test_Data/FkSpectra/' + fileName + ' ' + repoHome + stdsLocationInRepo + '/FkSpectra/')
fileName = os.listdir(testDataHome + '/Standard_Test_Data/FkSpectra/ChanSeg')[0]
os.system('cp ' + testDataHome + '/Standard_Test_Data/FkSpectra/ChanSeg/' + fileName + ' ' + repoHome + stdsLocationInRepo + '/FkSpectra/ChanSeg')
print('Complete')
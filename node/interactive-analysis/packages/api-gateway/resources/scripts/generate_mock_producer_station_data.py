'''
Creates the necessary json files for the mock producer based on the processing station group file in additional test data directory

Running the script:

python generate_mock_producer_station_data.py --outDir <outDir>
                         

Parameters: 

--outDir <outDir> - Path to the output directory where the created JSON files will be written

'''

import os
import json
import time
import argparse

def writeOutput(fileWithPath, data):
    with open(os.path.join(outDir,fileWithPath), 'w') as writeFile:
        writeFile.write(json.dumps(data, indent=4))
    writeFile.close()

def readAndProcessJson(fileWithPath, dataType):
    data = {}
    with open(fileWithPath) as jsonFile:
        data = json.loads(jsonFile.read())
    jsonFile.close()

    if (not data):
        print('Unable to read ' + dataType + ' data from file')
        exit(0)
    return data

parser = argparse.ArgumentParser()
parser.add_argument('--outDir', dest='outDir', help='Path to the output directory where the converted JSON files will be written')

args = parser.parse_args()

# Extract command-line arguments
outDir = args.outDir

print('Creating json files...')
fullStationGroups = readAndProcessJson('../test_data/additional-test-data/processing-station-group.json', 'station groups')
stationsGroupNames = readAndProcessJson('../test_data/additional-test-data/soh.station-groups.json', 'station group names')

# get groups and set priority
# groupsWithPriority = {}
# index = 1
# for group in stationsGroupNames['stationGroupNames']:
  # groupsWithPriority[group] = index
  # index += 1

# get station group map that shows group, priority, and station names
# creates a unique set of station names
# creates a unique set of stations
# stationGroupMap = []
allStations = []
allStationNames = set()
for group in fullStationGroups:
  groupName = group['name']
  stationsForGroup = [station['name'] for station in group['stations']]
  for station in group['stations']:
    allStationNames.add(station['name'])
    if station not in allStations:
      allStations.append(station)
  # stationGroupMap.append({
  #   'group': groupName,
  #   'priority': groupsWithPriority[groupName],
  #   'stations': stationsForGroup
  # })

# Write output
print('Writing data to: ' + outDir)
writeOutput('StationGroupMap.json', fullStationGroups)
writeOutput('Stations.json', allStations)
writeOutput('StationList.json', list(allStationNames))
print('Done')


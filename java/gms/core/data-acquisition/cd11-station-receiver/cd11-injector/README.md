# CD1.1 Injector/Repeater


Responsible for injecting CD1.1 data from per-station directories of RawStationDataFrames, or ingesting station data from a
Kafka broker and relaying to available consumers (via Connman/Dataman).

## How the injector finds data / stations

The injector expects either:
 - a top-level directory of data beneath which there are directories named by station. 
 - a configuration that specifies information for each station, e.g. name and location of its data.  In this case there does not need to be a common root directory.  

In both cases, the injector expects that in each station directory there will be Raw Station Data Frame json files.  Entries in those files, if there are more than one, are expected to be time-ordered.

## Repeater Operation

The repeater manager (`Cd11RepeaterManager.java`) connects to a Kafka broker specified in config, and subscribes to a 
single master topic for available RSDF data. When data is published to the broker, the repeater consumes this data and, 
depending on the station, either creates a new verticle to process this station's data if it has never been seen before, 
or simply ignores it. The repeater manager's responsibility is solely to track previously-seen stations, and create new 
repeater verticles lazily, which each handle exactly 1 station's data.

Each repeater verticle -upon creation- then subscribes to their specified station data in the Kafka broker (simply filtering 
messages that don't match the filter). The repeater also performs a handshake with connman to determine where that specific
station data should be sent. When relevant station data is received from the broker, it is simply handed off to the available
dataman consumer (assuming the connman handshake succeeded). If a repeater has not received a message matching its station
in some period of time (specified in config), it terminates and informs the repeater manager so that the lazy instantiation
is reset for that station name.

## Required Environment Variables

There are two important environment variables needed for operation of the repeater OR the injector.

  - `VERTX_CONFIG_PATH`: Where the injector/repeater config file is located. If none is specified, defaults
  to `conf/config.json`, *but be careful*. Config that doesn't match the operation mode (which **must** be specified) will
  result in a program runtime crash. Appropriate naming of the config files (ex: `conf/repeaterConfig.json`) is recommended.
  
  - `INJECTOR_OPERATION_MODE`: Should be set to either `injector` or `repeater` (case insensitive). Conflicts between this
  operation mode and the contents of the config file specified will result in a runtime crash (ex: injector mode specified,
  but repeater config provided).

## Injector Configuration

The CD1.1. injector is driven by json config, whose path is provided by the `VERTX_CONFIG_PATH` env var.

Here are the keys:
  - `useConnman`: whether the injector should ask connman where to inject a particular station or whether the station fields specify it

  - `connManAddress`:  address or ip on which ConnMan is running
  - `connManPort`: port on which connMan is running


  - `preStartDelaySeconds`: delay before injection start.  The Injector Manager will add this to now() and provide it to all station injectors.  This should be set such that the injectors have enough time to do their setup (ordering the files by time + preprocessing) within this time interval. 
  - `maxConcurrentOpenFiles`: max number of open files allowed per station injector when trying to figure out how to time-order the files.  This number * the number of stations will be roughly the number of concurrently opened files.  If you have a significantly large limit and lots of data files, increasing this value will improve startup time and will allow the pre-start delay to be reduced.  For the pedantic, this isn't actually a hard limit on files, but a limit on the parallelism of tasks that open files.
  - 'meterDataStream': a setting intended for test. If true, inject at the same the data was originally received; false, inject as fast as possible.  In both cases the data times will be changed to the current time frame, so if 'meterDataStream' is false receivers of the data will see data times in the future.
  - `referenceTime`: time at or earlier than all reception times across all stations.  This is mapgped to the injection start time in the current time frame for correct timestamp correction and data metering.

  - `rsdfDiscoveryBasePath`: top-level directory of the RawStationDataFrame (rsdf) data to inject for all stations
  - `useBasePathDiscovery`: true: should the station data be discovered by traversing from the base path; or false: only using station paths defined explicitly in config
  - `loopDataStream`: whether all stations should independently loop their data streams when finished


  - `frameDestination`: frame destination data per Cd11SocketConfig
  - `frameCreator`: frame creator information per Cd11SocketConfig

  - `stations`: station objects as follows
    - `id`: station name
    - `enabled`: actually inject data for this station
    - `dataPath`: optional location of data to inject for this station; needed if useBasePathDiscovery is false
    - `consumerAddress`: optional ip/address of the data consumer for this station's data; needed if useConnMan is false
    - `consumerPort`: optional port of the data consumer for this station's data; needed if useConnMan is false
    
## Repeater Configuration
The CD1.1. repeater is driven by json config, whose path is provided by the `VERTX_CONFIG_PATH` env var.

Here are the keys:
  - `runDebugKafkaPublisher`: whether the debug Kafka *publisher* should be ran or not. Used for testing when the broker
  is local and needs to be populated
  
  - `repeaterTimeoutSeconds`: if a repeater has not received its specific station data in this long, then terminate for
  that station
    
  - `kafkaConsumerTopic`: the topic that RSDF data will be published to on the kafka broker (ex: `soh.rsdf`)
      
  - `kafkaConsumerID`: a tag/"UUID" to be appended for all commits to the kafka broker upon message consumption. **Separate 
  instances of the repeater that consume from the _same broker_ should use unique tags**, failure to do so will result 
  in inconsistent message receiving and/or race conditions between distinct repeaters
    
  - `kafkaBrokerAddress`: address or ip for the kafka broker that the repeater will subscribe to.
  - `kafkaBrokerPort`: port of the kafka broker that will be subscribed to

  - `connManAddress`:  address or ip on which ConnMan is running
  - `connManPort`: port on which connMan is running

  - `frameDestination`: frame destination data per Cd11SocketConfig
  - `frameCreator`: frame creator information per Cd11SocketConfig

## Injector Configuration gotchas

Note that if you supply an in appropriate data reference time, you may have to wait a very long time for the data to inject.

Finding a reference time can be done by running:

```bash
find . -name \*json -exec sh -c 'python -m json.tool $0' {} \; | grep receptionTime | sort | tee reception-times | head -n1
```
in the root directory of the data.

## Injector Configuration and Notes for the Sample Data Sets 10for2 and 20for24
 
- These sample data sets have enough files that vert.x will complain about the event loop being blocked.  This is due to blocking calls to read the directory contents of each station, to stat those files, and to read those files.  This complaining by vert.x is normal.  We don't care that the event loop is blocked because we're only doing that during injector startup when there is nothing else useful to do.


#### Injector Configuration for sample data sets

As always in tuning, your mileage may vary.  These numbers were derived on a 2-core Linux vm with a file descriptor limit of 1024 on a somewhat underpowered 2016 MacBookPro.

#### 20for24

This is the default configuration: 240 seconds of preStartDelay and maxConcurrency of 40. Processing the ~420K files in 20for24 takes roughly 3 minutes, i.e. 180 seconds; the default configuration has added some margin.  Max concurrency of 40 keeps below the default file descriptor limit of 1024 (20 * 40 = 800 < 1024) by a significant margin.  Note that the file descriptor limit is per process, which in this case is per-jvm; each verticle (each station injector) is not it's own jvm.

#### 10for2

This takes roughly 15 seconds preStartDelay with maxConcurrency of 40.


## Injector/Repeater Running and usage

From gradle use the runShadow task:

```bash
# no arguments
gradle :cd11-injector:runShadow

```
Note that default config is in `conf/config.json` in the root of the cd11-injector project, but it is recommended to use
the `VERTX_CONFIG_PATH` env var.

Or provide your own config using a command line argument:

```
gradle :cd11-injector:runShadow --args="-conf /path/to/your/config.json"
```

From docker:
Mount you data on the host into `/rsdf` in the container. 

```bash
docker run -it --net=host -v/path/to/your/data/:/rsdf ocal/gms-common/cd11-injector:latest
```

If you want to override config you'll have to mount it in as well:

Either to where the default conf is (`/cd11-injector/confg`):

```bash
docker run -it --net=host -v/path/to/config:/cd11-injector/conf/ -v/path/to/your/data/:/rsdf ocal/gms-common/cd11-injector:latest bin/cd11-injector
```

Or you can mount it somewhere else in the container and provide it as an argument to the injector:

```bash
docker run -it --net=host -v/path/to/config:/tmp -v/path/to/your/data/:/rsdf ocal/gms-common/cd11-injector:latest bin/cd11-injector -conf /tmp/config.json
```

Note you can also provide a json string as a config as well with the `-conf` flag.

### Advanced Usage

The shadowJar and the docker instantiation (which uses a fat jar) both provide default arguments to the Vert.x launcher. 
You can interact with the launcher directly via:

```
gradle :cd11-injector:run --help
```

With the fatjar you are providing arguments to the launcher so in docker you can:

```
docker run -it local/gms-common/cd11-injector:latest bin/cd11-injector --help
```

## Troubleshooting

### The injectors aren't doing anything except sending ack/nacks

You probably have a data reference time problem and the injection will happen some time in the far future.

Check that your reference time is set correctly at or earlier than the earlies reception time in the data.

You can also see when the next data will be sent by grepping / looking in the logs for "Next send data scheduled for":

E.g.
```
11:00:10.480 [vert.x-eventloop-thread-3] INFO  class gms.dataacquisition.stationreceiver.cd11.injector.InjectionDataHandler|PLCA - Next send data scheduled for: 2019-12-18T18:03:47.491860Z - PT3M37.011085S
11:00:21.932 [vert.x-eventloop-thread-3] INFO  class gms.dataacquisition.stationreceiver.cd11.injector.InjectionDataHandler|EKA - Next send data scheduled for: 2019-12-18T18:43:51.492144Z - PT43M29.559295S

```
Those messages indicate PLCA will inject in 3m47sec, EKA in 43m29 seconds.  Both of those may be reasonable given pre-start delays and variance in the data.  Values in days probably indicate you have a bad reference time.


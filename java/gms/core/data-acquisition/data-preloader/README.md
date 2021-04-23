# SOH Data Generator and PreLoader

### Notes

1. It currently takes about 24 hours for 30 days of Analog and Boolean ACEI, Capability Rollups, RSDFs, and Station SOH to load. Please keep in mind that this loads over 90 million records into the database.
2. By default, data is generated for the past 30 days. Please update the script to change the total generation duration.
3. This solution is currently a two, or three, step process: `gmsctl` + loader script + optionally starting an injector.
4. The `data-preloader` service does not start by default when using the `docker-compose-swarm-30-day-soh.yml` compose file located in `<gms common main>/docker-compose-swarm`. Scaling this up at any time will start the data load.
   * Compose File Variables:
      * `STATION_GROUP`: the station group the preloader will generate data for
      * `DAYS_TO_LOAD`: the number of days the preloader will generate data for
      * `DAYS_AGO_TO_START`: the number of days ago the preloader will use as the start date for the generated data
      * `LOAD_RSDFS`: the rsdf load will run if this is set to "TRUE"
      * `RECEPTION_DELAY`: the reception delay to be used when generating rsdfs
      * `RSDF_SAMPLE_DURATION`: the sample duration to be used when generating rsdfs
      * `LOAD_STATION_SOHS`: the station soh load will run if this is set to "TRUE"
      * `STATION_SOH_SAMPLE_DURATION`: the sample duration to be used when generating station sohs
      * `LOAD_ANALOG_ACEIS`: the analog acei load will run if this is set to "TRUE"
      * `ACEI_ANALOG_SAMPLE_DURATION`: the sample duration to be used when generating analog aceis
      * `LOAD_BOOLEAN_ACEIS`: the boolean acei load will run if this is set to "TRUE"
      * `ACEI_BOOLEAN_SAMPLE_DURATION`: the sample duration to be used when generating boolean aceis
      * `LOAD_ROLLUPS`: the rollup load will run if this is set to "TRUE"
      * `ROLLUP_SAMPLE_DURATION`: the sample duration to be used when generating rollups
      * `JAVA_OPTS: -Dreactor.schedulers.defaultPoolSize=`: places an upper limit on the number of threads that will be used by the preloader 
5. The `soh-control` service does not start by default when using the `docker-compose-swarm-30-day-soh.yml` compose file located in `<gms common main>/docker-compose-swarm`. That can be scaled up at any time after the data load. 
6. The `frameworks-osd-ttl-worker` service does not start by default when using the `docker-compose-swarm-30-day-soh.yml` compose file located in `<gms common main>/docker-compose-swarm`. That can be scaled up at any time after the data load.
7. Use `docker-compose-swarm-30-day-soh.yml` as an example.

### Running
Please use this if you do not need additional flexibility as offered below in the "Running" section. This will copy the desired compose file to the directory that contains `load-soh-data.sh`, run `gmsctl`, and run the data load. It will not scale up any data injectors or `frameworks-osd-ttl-worker`.
1. Source `env.sh` for the desired testbed. 
2. run `gmsctl deployment deploy -c <full path to compose file> -b <branch name> <deployment name>`
3. Scale up `data-preloader` to start the dataloader `gmsctl service scale <deployment name>_data-preloader 1`.
4. Optionally scale up `soh-control`. Please be aware of what the `soh-control` does.
5. Optionally run a data injector.
6. Optionally scale up `frameworks-osd-ttl-worker`. Please be aware of what the `frameworks-osd-ttl-worker` does.

# Using the test data set create/upload scripts

These scripts provide the ability to build the test data set as a set of JSON files, and to upload that group of JSON files to the appropriate services for storage into GMS.

## The input data to create the test data set

See GMS wiki: Standard Test Data Set

## Creating a test data set

`create-test-data-set.sh` is a Bash script with one required argument indicating where the files used to create the data set are.  
It writes the output JSON files of the data set to a directory 'gms_test_data_set' under the input directory.  

Example usage:

```bash
./create-test-data-set.sh /some/directory
# writes files to /some/directory/gms_test_data_set/
```

The script builds the test data set by running various applications with `gradle` and passing arguments to those invocations 
that make them write their data to the specified output directory.  The script looks for environment variable `GMS_HOME`
to know where to run these gradle commands from (the top-level of the java code in the repo); if it isn't set, the script
will try running them from a location relative to the script itself (e.g. `../../../../`).

Running the script elsewhere from where it currently resides can be done like so:
```bash
GMS_HOME=/<your_path>/gms-common/java ./create-test-data-set.sh /<your_path>/standard_test_data_set
```

## Uploading a test data set to the OSD/databases

`upload-test-data-set.sh` is a Bash script with three required arguments: the directory with the GMS JSON files, the directory with the FKSpectra files, the directory with the binary waveform (.w) files.
It stores the data in these files to GMS/OSD databases.  This script calls `coi-data-loader` (a Java program) which uses GMS system configuration
to determine how to connect to GMS databases.  This means either the GMS system configuration mechanism (i.e. etcd) needs to be accessible
or you need to have the special file `~/configuration-overrides.properties` 
with appropriate values in it for e.g. `sql_url` (to set (Postgre)SQL hostname) and `cassandra_connect_points` (to set Cassandra hostname).

Example usage:
```bash
./upload-test-data-set.sh /<your_path>/standard_test_data_set/gms_test_data_set /<your_path>/standard_test_data_set/FkSpectra/ChanSeg /<your_path>/standard_test_data_set/w
```
# Command-Line Utilities

The following GMS (Geophysical Monitoring System) command-line
utilities are [available here](../bin):

* [**gmskube**](#gmskube): Manage running instances of the GMS system on a Kubernetes cluster
* [**gms-css-to-coi**](#gms-css-to-coi): Convert CSS station reference data to OSD format usable by GMS

## gmskube

The **gmskube** command-line program is used to install and configure
*instances* of the GMS (Geophysical Monitoring System) system on
Kubernetes.

Each *instance* is an install of a multi-container application that is
managed as a single unit and runs on a Kubernetes cluster. Each
instance is contained within its own namespace in Kubernetes. The only
type of instance available is 'soh'.

Multiple copies of **soh** instances may be run simultaneously. Each
instance must be given a unique name to identify it as well as
distinguish it from other running instances of the same type. For example, 
one instance of **soh** may be running as 'soh-develop' while another
instance of **soh** may be running as 'soh-integration'.

Different versions of a instance type may be available from the configured
Docker registry. Released versions of GMS are tagged with a specific version
number.

#### Configuration
Before you can run **gmskube**, you must first download a Kubeconfig bundle 
from the cluster and have the kubectl context set to the correct cluster.

 1. Login to Rancher
 2. Click the cluster name
 3. In the upper right, click the blue Kubeconfig File button
 4. Copy/Paste the contents into ~/.kube/config on your development machine
 5. If you have kubectl installed, the KUBECONFIG environment variable should
    already be set.  If not, set KUBECONFIG=~/config

#### Subcommands

* **gmskube ls** <br>
  List all currently running instances.  The *name* and *type* of each instance 
  will be listed along with other information such as the *user* that installed 
  the instance, when it was last *updated*, and what *tag* it was based on.
  ```bash
  # list all running instances
  gmskube ls
  ```

* **gmskube install** [*options*] *name* <br>
  Install an instance of a given system type based on the specified 
  tagged version.
  ```bash
  # install an instance of soh with the name soh-develop
  gmskube install --tag release-13.1 --type soh soh-develop
  ```
  - **--tag** *NAME* <br>
    *REQUIRED* | Tag name, which corresponds to the docker tag of the images.
  - **--type** *{soh,logging,grafana,sb}* <br>
    *REQUIRED* | Type of instance.
  - [**--config** *CONFIGDIR*] <br> 
    *OPTIONAL* | Path to a directory of configuration overrides to load into this instance.
  - [**--set** *NAME=VALUE*] <br>
    *OPTIONAL* | Set a value in the chart to the specified value. May
    be specified multiple times for different values.
    Example: *--set foo=bar* to set value *foo* to *bar*.
  - [**--livedata**]<br>
    *OPTIONAL* | Include live data in the instance.  This option is mutually 
    exclusive with **--injector**.
  - [**--connman-port** *CONNMAN_PORT*]<br>
    *OPTIONAL* | If specified, sets the environment variable to change the 
    well-known port for the CD11 connman service, and configures the
    port in kubernetes to be externally visible.
  - [**--connman-data-manager-ip** *CONNMAN_DATA_MANAGER_IP*]<br>
    *OPTIONAL* | If specified, sets the environment variable to change
    the external IP address of the CD11 dataman service.
  - [**--connman-data-provider-ip** *CONNMAN_DATA_PROVIDER_IP*]<br>
    *OPTIONAL* | If specified, sets the environment variable to change
    IP address of the data provider sending data to the CD11 dataman service.
  - [**--dataman-ports** *DATAMAN_PORTS*]<br>
    *OPTIONAL* | If specified, sets the environment variable to change
    the port range for the CD11 dataman service, and configures the ports
    in kubernetes to be externally visible.
  - [**--injector**]<br>
    *OPTIONAL* | Include the data injector in the instance. This option is
    mutually exclusive with **--livedata**.
  - [**--injector-dataset** *INJECTOR_DATASET*]<br>
    *OPTIONAL* | Dataset for the injector to use when **--injector** is
    specfied. If not specified, thedefault is "81for10min".
<br>
   
* **gmskube upgrade** [*options*] *name* <br>
  Upgrade a running instance with changes to state or to install updated images.
  ```bash
  # start the injector for the soh-develop instance
  gmskube upgrade --injector soh-develop
  ```
  - [**--set** *NAME=VALUE*] <br>
    *OPTIONAL* | Set a value in the chart to the specified value. May
    be specified multiple times for different values.
    Example: *--set foo=bar* to set value *foo* to *bar*.
  - [**--livedata**]<br>
    *OPTIONAL* | Include live data in the instance.  This option is mutually 
    exclusive with **--injector**.
  - [**--connman-port** *CONNMAN_PORT*]<br>
    *OPTIONAL* | If specified, sets the environment variable to change the 
    well-known port for the CD11 connman service, and configures the
    port in kubernetes to be externally visible.
  - [**--connman-data-manager-ip** *CONNMAN_DATA_MANAGER_IP*]<br>
    *OPTIONAL* | If specified, sets the environment variable to change
    the external IP address of the CD11 dataman service.
  - [**--connman-data-provider-ip** *CONNMAN_DATA_PROVIDER_IP*]<br>
    *OPTIONAL* | If specified, sets the environment variable to change
    IP address of the data provider sending data to the
    CD11 dataman service.
  - [**--dataman-ports** *DATAMAN_PORTS*]<br>
    *OPTIONAL* | If specified, sets the environment variable to change
    the port range for the CD11 dataman service, and configures the ports
    in kubernetes to be externally visible.
  - [**--injector**]<br>
    *OPTIONAL* | Include the data injector in the instance. This option is
    mutually exclusive with **--livedata**.
  - [**--injector-dataset** *INJECTOR_DATASET*]<br>
    *OPTIONAL* | Dataset for the injector to use when **--injector** is
    specfied. If not specified, thedefault is "81for10min".
  - [**--tag** *NAME*] <br>
    *OPTIONAL* | Tag name, which corresponds to the docker tag of the images to
    upgrade.  If not specified, the currently installed tag name will be used.
<br>

* **gmskube uninstall** *name* <br>
  Uninstall a running instance with the given name. 
  ```bash
  # uninstall the soh-develop instance
  gmskube uninstall soh-develop
  ```
<br>

* **gmskube reconfig** *--config CONFIGDIR* *name* <br>
  Reconfigure a running instance with updated configuration. Portions of the system
  affected by the configuration change will be stopped and then restarted after
  the updated configuration has been loaded.
  ```bash
  # update the configuration in the soh-develop instance
  gmskube reconfig --config updated-config soh-develop
  ```
  - [**--config** *CONFIGDIR*] <br> 
    *REQUIRED* | Path to a directory of configuration to load into this instance.
  - [**--tag** *NAME*] <br>
    *OPTIONAL* | Tag name, which corresponds to the docker tag of the images to
    upgrade.  If not specified, the currently installed tag name will be used.

## gms-css-to-coi

To update station reference and station processing configuration, any files in 
the native CSS specification must first be converted to the GMS Common Object 
Interface (COI) format.  This conversion must be run before the config can 
be ingested by the GMS system for either a **gmskube install** or 
a **gmskube reconfig** when run with the **--config** argument.

By convention, the source CSS station-reference data is typically in 
a **data** directory and the resulting COI data is generated to a 
**stationdata* directory.

```bash
# convert the COI data directory to an OSD stationdata directory
gms-css-to-coi -s path-to-my-config/station-reference/data -d path-to-my-config/station-reference/stationdata
```
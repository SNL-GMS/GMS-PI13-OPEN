# Kafka
This image is based on centos7 and is running Kafka.

## Getting Started

### Prerequisities
In order to run this container you'll need docker installed.

* [Windows](https://docs.docker.com/docker-for-windows/install/)
* [OS X](https://docs.docker.com/docker-for-mac/install/)
* [Linux](https://docs.docker.com/install/linux/docker-ce/ubuntu/)

## JMX Exporter

#### Terminology
* [JMX](https://docs.oracle.com/javase/tutorial/jmx/index.html) (Java Management Extension) is a standard part of the Java Platform which provides an architecture to manage resources dynamically at runtime. JMX is used  to make the system configurable or to get the state of application at any point of time. To manage any resource through JMX, we need to interact with Managed Beans (MBeans).

* **MBeans** (Managed Beans) are Java objects that represent a manageable resource such as an application, service, component, or device for real-time management of applications.

* **Java Instrumentation API** is an API on the JVM which provides the ability to add byte-code to existing compiled Java classes.

* **Java Agent** is just a specially crated jar file which utilizes the Instrumentation API to alter existing byte-code that is loaded to the JVM.

#### JMX to Prometheus Exporter
The [JMX to Prometheus exporter](https://github.com/prometheus/jmx_exporter) is a collector authored by the Prometheus team that can configurably scrape and expose mBeans of a JMX target. In this context, it is being used to collect metrics from Kafka.

The current implementation is run with a Java Agent. While the exporter can also be run as an independent HTTP server and scrape remote JMX targets, this has various disadvantages (being more difficult to configure and being unable to expose process metrics such as memory and CPU usage).

#### Building and Configuration
When building the docker container, the exporter `.jar` file will be installed and a local configuration file specific to kafka metrics (`kafka.yml`) will be copied in. The `kafka.yml` file can be configured to expose additional metrics.

* Other available metrics to monitor are listed [here](https://docs.confluent.io/current/kafka/monitoring.html)
* Examples of various JMX configuration files can be found [here](https://github.com/prometheus/jmx_exporter/tree/master/example_configs).

#### ARG Variables
The JMX exporter requires the following variables to be assigned in order to expose metrics:
- jmx_exporter_version: version of the exporter to be installed (defaults to `0.12.0`)
- metrics_port: port to expose metrics (defaults to `7071`)
- enable_metrics: runs the exporter using javaagent and the supplied configuration file (defaults to `"off"`, set to `"on"` to enable)
![GMS Logo](doc/images/gms-logo.png)

# GMS Common

This repository contains the common code for the **Geophysical Monitoring System (GMS)**.

Source code is organized by language:
* Java code has been incorporated in the [java](java) subdirectory.
* Python code has been incorporated in the [python](python) subdirectory.
* Typescript code has been incorporated in the [node](node) subdirectory.

## Installation Instructions

Instructions for building and running the system can be [**found here**](doc/).

## GMS  State-of-Health (SOH) Monitoring

The GMS Station State-of-Health (SOH) Monitoring provides a suite of
displays showing near real-time information about the state-of-health
of individual stations and station groups in use on the System.  These
displays provide support monitoring, detecting, and troubleshooting
problems with station data availability and quality.

Each station is given a status color (green = good SOH, yellow =
marginal SOH, red = bad SOH) based on whether the data from that
station meets configured thresholds over a configured period of time
for characteristics such as data latency, amount of missing data, and
environmental issues. SOH issues on individual channels roll up to
provide an overall SOH status color for that station. Overview
displays provide high-level monitoring and notification of issues
while more detailed displays allow the user to drill-down into more
in-depth information for troubleshooting.

### GMS SOH Persistent Services

| **Service Name** | **Description** |
|---|:---|
| da-connman                                  | Connection Manager for accepting connections for incoming CD1.1 data |
| da-dataman                                  | Data Manager for reading CD1.1 data |
| capability-soh-rollup-kafka-consumer        | Captures SOH Rollup data and stores it to the OSD |
| cd11-rsdf-processor                         | Decodes incoming CD1.1 Raw Station Data Frames (RSDF) |
| config-loader                               | Service for orchestrating configuration loading |
| frameworks-configuration-service            | Serves processing configuration |
| frameworks-osd-service                      | Object Storage and Distribution (OSD) service |
| frameworks-osd-rsdf-kafka-consumer          | Captures RSDF data and stores it to the OSD |
| frameworks-osd-station-soh-kafka-consumer   | Captures computed SOH statistics and stores them to the OSD |
| frameworks-osd-systemmessage-kafka-consumer | Captures system messages and stores them to the OSD |
| frameworks-osd-ttl-worker                   | Periodically deletes the oldest data from the OSD |
| interative-analysis-api-gateway             | Backend services for the GMS user interface |
| interative-analysis-config-service          | Serves configuration data for the interactive analysis services |
| interative-analysis-ui                      | Serves the GMS user interface |
| acei-merge-processor                        | Merges and consolidates acquired channel environmental issues data |
| soh-control                                 | Computes State of Health (SOH) statistics from incoming RSDF metadata |
| ssam-control                                | Manages Station State of Health acknowlegement and quieting |
| soh-quieted-list-kafka-consumer             | Captures quieted issue lists and stores them to the OSD |
| soh-status-change-kafka-consumer            | Captures status changes and stores them to the OSD |

### GMS SOH Third-Party Services

| **Service Name** | **Description** |
|---|:---|
| bastion                 | Contains command-line support tools for system maintainence |
| etcd                    | Service for system configuration values |
| kafka[1-3]              | Distributed streaming queues used for interprocess communication |
| postgresql-gms          | The database used for storing OSD objects |
| postgresql-exporter     | Collects database metrics |
| traefik                 | Network routing service |
| zoo                     | Zookeeper key-value service used by kafka |


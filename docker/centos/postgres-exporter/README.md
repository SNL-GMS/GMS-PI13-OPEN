# Postgres Exporter
This image is based on centos7 and is running Postgres exporter. By default, it exposes metrics on port 9187.

## Getting Started

### Prerequisities
In order to run this container you'll need docker installed.

* [Windows](https://docs.docker.com/windows/started)
* [OS X](https://docs.docker.com/mac/started/)
* [Linux](https://docs.docker.com/linux/started/)

### Usage
Postgres collects a lot of metrics and offers it with the `pg_stat` system views ([documentation](https://wiki.postgresql.org/wiki/Monitoring#PostgreSQL_builtin_.26_contrib)). In order for the exporter collect certain metrics from Postgres as a non-superuser, you have to create functions and views as a superuser and assign permissions to the Postgres exporter user. See the Postgres exporter [documentation](https://github.com/wrouesnel/postgres_exporter#running-as-non-superuser) for an example script that does this. If you connect using superuser credentials, no extra configuration is necessary.

#### Container Parameters
The entrypoint for this container is the Postgres exporter binary. You can find the documentation for options [here](https://github.com/wrouesnel/postgres_exporter#flags).

#### Build Args

- PG_EXPORTER_VERSION: the version of Postgres exporter to download and run.

#### Environment Variables
The Postgres exporter requires the following environment variables to be set in order to connect to a Postgres database:
- DATA_SOURCE_URI: a raw URI in the form `host:port/database_name`
  - If the Postgres instance doesn't support SSL: `host:port/db_name?sslmode=disable`
- DATA_SOURCE_USER: The username to connect with.
- DATA_SOURCE_PASS: The password to connect with.
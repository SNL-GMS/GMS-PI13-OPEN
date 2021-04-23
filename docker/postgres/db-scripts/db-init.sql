select 'create database gms' where not exists (select from pg_database where datname ='gms')\gexec

create role gms_admin with noinherit login encrypted password 'gmsdb:postgres:gms_admin:over-realized-exclusivism';

create role gms_read_only with noinherit login encrypted password 'gmsdb:postgres:gms_read_only:humoured-tempered-furious-lion';

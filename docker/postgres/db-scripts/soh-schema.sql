create schema if not exists gms_soh;

-- TODO (sgk 1/24/2020) Figure out a better way to handle testing
create schema if not exists gms_soh_test;

comment on schema gms_soh is 'GMS State of Health Schema';

-- Enum Types
create type rsdf_payload_format as enum ('CD11', 'IMS20_SOH', 'IMS20_WAVEFORM', 'SEEDLINK', 'MINISEED');
create type authentication_status as enum('NOT_APPLICABLE', 'AUTHENTICATION_FAILED', 'AUTHENTICATION_SUCCEEDED', 'NOT_YET_AUTHENTICATED');
create type soh_status_enum as enum('GOOD', 'MARGINAL', 'BAD');
create type system_message_type_enum as enum(
    'STATION_NEEDS_ATTENTION', 'STATION_SOH_STATUS_CHANGED',
    'STATION_CAPABILITY_STATUS_CHANGED', 'STATION_GROUP_CAPABILITY_STATUS_CHANGED',
    'CHANNEL_MONITOR_TYPE_STATUS_CHANGED', 'CHANNEL_MONITOR_TYPE_STATUS_CHANGE_ACKNOWLEDGED',
    'CHANNEL_MONITOR_TYPE_QUIETED', 'CHANNEL_MONITOR_TYPE_QUIET_PERIOD_CANCELED',
    'CHANNEL_MONITOR_TYPE_QUIET_PERIOD_EXPIRED');
create type system_message_category_enum as enum('SOH');
create type system_message_sub_category_enum as enum('STATION', 'CAPABILITY', 'USER');
create type system_message_severity_enum as enum('CRITICAL', 'WARNING', 'INFO');
create type station_aggregate_type_enum as enum('LAG', 'TIMELINESS', 'MISSING', 'ENVIRONMENTAL_ISSUES');
create type soh_monitor_type_enum as enum('LAG',
                                                  'MISSING',
                                                  'TIMELINESS',
                                                  'ENV_AMPLIFIER_SATURATION_DETECTED',
                                                  'ENV_AUTHENTICATION_SEAL_BROKEN',
                                                  'ENV_BACKUP_POWER_UNSTABLE',
                                                  'ENV_BEGINNING_DATE_OUTAGE',
                                                  'ENV_BEGINNING_TIME_OUTAGE',
                                                  'ENV_CALIBRATION_UNDERWAY',
                                                  'ENV_CLIPPED',
                                                  'ENV_CLOCK_DIFFERENTIAL_IN_MICROSECONDS',
                                                  'ENV_CLOCK_DIFFERENTIAL_TOO_LARGE',
                                                  'ENV_CLOCK_LOCKED',
                                                  'ENV_DATA_AVAILABILITY_MINIMUM_CHANNELS',
                                                  'ENV_DATA_AVAILABILITY_GEOPHYSICAL_CHANNELS',
                                                  'ENV_DATA_AVAILABILITY_GEOPHYSICAL_CHANNELS_UNAUTHENTICATED',
                                                  'ENV_LAST_GPS_SYNC_TIME',
                                                  'ENV_DEAD_SENSOR_CHANNEL',
                                                  'ENV_DIGITAL_FILTER_MAY_BE_CHARGING',
                                                  'ENV_DIGITIZER_ANALOG_INPUT_SHORTED',
                                                  'ENV_DIGITIZER_CALIBRATION_LOOP_BACK',
                                                  'ENV_DIGITIZING_EQUIPMENT_OPEN',
                                                  'ENV_DURATION_OUTAGE',
                                                  'ENV_ENDING_DATE_OUTAGE',
                                                  'ENV_ENDING_TIME_OUTAGE',
                                                  'ENV_END_TIME_SERIES_BLOCKETTE',
                                                  'ENV_EQUIPMENT_HOUSING_OPEN',
                                                  'ENV_EQUIPMENT_MOVED',
                                                  'ENV_EVENT_IN_PROGRESS',
                                                  'ENV_GAP',
                                                  'ENV_GLITCHES_DETECTED',
                                                  'ENV_GPS_RECEIVER_OFF',
                                                  'ENV_GPS_RECEIVER_UNLOCKED',
                                                  'ENV_LONG_DATA_RECORD',
                                                  'ENV_MAIN_POWER_FAILURE',
                                                  'ENV_MAXIMUM_DATA_TIME',
                                                  'ENV_MEAN_AMPLITUDE',
                                                  'ENV_MISSION_CAPABILITY_STATISTIC',
                                                  'ENV_NEGATIVE_LEAP_SECOND_DETECTED',
                                                  'ENV_NUMBER_OF_CONSTANT_VALUES',
                                                  'ENV_NUMBER_OF_DATA_GAPS',
                                                  'ENV_NUMBER_OF_SAMPLES',
                                                  'ENV_OUTAGE_COMMENT',
                                                  'ENV_PERCENT_AUTHENTICATED_DATA_AVAILABLE',
                                                  'ENV_PERCENT_DATA_RECEIVED',
                                                  'ENV_PERCENT_UNAUTHENTICATED_DATA_AVAILABLE',
                                                  'ENV_PERCENTAGE_GEOPHYSICAL_CHANNEL_RECEIVED',
                                                  'ENV_POSITIVE_LEAP_SECOND_DETECTED',
                                                  'ENV_QUESTIONABLE_TIME_TAG',
                                                  'ENV_ROOT_MEAN_SQUARE_AMPLITUDE',
                                                  'ENV_SHORT_DATA_RECORD',
                                                  'ENV_SPIKE_DETECTED',
                                                  'ENV_START_TIME_SERIES_BLOCKETTE',
                                                  'ENV_STATION_EVENT_DETRIGGER',
                                                  'ENV_STATION_EVENT_TRIGGER',
                                                  'ENV_STATION_POWER_VOLTAGE',
                                                  'ENV_STATION_VOLUME_PARITY_ERROR_POSSIBLY_PRESENT',
                                                  'ENV_TELEMETRY_SYNCHRONIZATION_ERROR',
                                                  'ENV_TIMELY_DATA_AVAILABILITY',
                                                  'ENV_TIMING_CORRECTION_APPLIED',
                                                  'ENV_VAULT_DOOR_OPENED',
                                                  'ENV_ZEROED_DATA');

set search_path to gms_soh;

create table if not exists acquisition_soh_status
(
	id bigint not null
		constraint acquisition_soh_status_pkey
			primary key,
	completeness double precision not null,
	completeness_summary varchar(255) not null,
	latency bigint not null,
	latency_summary varchar(255) not null
);

create table if not exists calibration
(
	id bigint not null
		constraint calibration_pkey
			primary key,
	calibration_factor_error double precision not null,
	calibration_factor_units varchar(255) not null,
	calibration_factor_value double precision not null,
	calibration_period_sec double precision not null,
	calibration_time_shift bigint not null
);

create table if not exists channel
(
	name varchar(255) not null
		constraint channel_pkey
			primary key,
	canonical_name varchar(255) not null,
	channel_band_type varchar(255),
	channel_data_type varchar(255),
	channel_instrument_type varchar(255),
	channel_orientation_code char,
	channel_orientation_type varchar(255),
	description varchar(255) not null,
	depth double precision,
	elevation double precision,
	latitude double precision,
	longitude double precision,
	nominal_sample_rate_hz double precision,
	horizontal_angle_deg double precision not null,
	vertical_angle_deg double precision not null,
	processingdefinition jsonb,
	processingmetadata jsonb,
	units varchar(255)
);

create table if not exists channel_configured_inputs
(
	id integer not null
		constraint channel_configuredinputs_pkey
			primary key,
	channel_name varchar(255) not null
		constraint fkkh5s7dif4016k33wf9ae66w3
			references channel,
	related_channel_name varchar(255) not null
		constraint fk16jwp1gsjwiwepwuap6cs0bp7
			references channel
);

create index channel_configured_inputs_channel_name on channel_configured_inputs (channel_name);
create index channel_configured_inputs_related_channel_name on channel_configured_inputs (related_channel_name);

create table if not exists channel_env_issue_analog
(
	id uuid not null
		constraint channel_env_issue_analog_pkey
			primary key,
	natural_id integer not null,
	channel_name varchar(255) not null
		     constraint channel_env_issue_analog_channel
		     		references channel,
	end_time timestamp with time zone not null,
	start_time timestamp with time zone not null,
	status double precision not null,
	type varchar(255) not null,
	constraint analog_channel_type_start
		unique (channel_name, type, start_time)
);
create index analog_channel_type_start_idx
	on channel_env_issue_analog (channel_name, type, start_time);
create index analog_acei_natural_id_idx
	on channel_env_issue_analog (natural_id);
		
create table if not exists channel_env_issue_boolean
(
	id uuid not null
		constraint channel_env_issue_boolean_pkey
			primary key,
	natural_id integer not null, 
	channel_name varchar(255) not null
		constraint channel_env_issue_boolean_channel
     		references channel(name),
	end_time timestamp with time zone not null,
	start_time timestamp with time zone not null,
	status boolean not null,
	type varchar(255) not null,
	constraint boolean_channel_type_start
		unique (channel_name, type, start_time)
);

create index boolean_channel_type_start_idx
	on channel_env_issue_boolean (channel_name, type, start_time);
create index boolean_acei_natural_id_idx
	on channel_env_issue_boolean (natural_id);
		
create table if not exists environment_soh_status
(
	id bigint not null
		constraint environment_soh_status_pkey
			primary key
);

create table if not exists environment_soh_counts_by_type
(
	environment_soh_status_id bigint not null
		constraint fkhajylh94eew3pwhhjjwwgj93r
			references environment_soh_status,
	soh_count integer,
	acquired_channel_env_issue_type varchar(255) not null,
	constraint environment_soh_counts_by_type_pkey
		primary key (environment_soh_status_id, acquired_channel_env_issue_type)
);

create table if not exists environment_soh_status_summaries
(
	environment_soh_status_id bigint not null
		constraint fk91gimbxx2oka6ig8g15003m3d
			references environment_soh_status,
	summary varchar(255),
	acquired_channel_env_issue_type varchar(255) not null,
	constraint environment_soh_status_summaries_pkey
		primary key (environment_soh_status_id, acquired_channel_env_issue_type)
);

create table if not exists frequency_amplitude_phase
(
	id bigint not null
		constraint frequency_amplitude_phase_pkey
			primary key,
	amplitude_response double precision[],
	amplitude_response_standard_deviation double precision[],
	amplitude_response_units varchar(255),
	frequencies double precision[],
	phase_response double precision[],
	phase_response_standard_deviation double precision[],
	phase_response_units varchar(255)
);

create table if not exists raw_station_data_frame
(
	id uuid not null
		constraint raw_station_data_frame_pkey
			primary key,
	payload_format public.rsdf_payload_format,
	authentication_status public.authentication_status not null,
	payload_data_end_time timestamp not null,
	payload_data_start_time timestamp not null,
	raw_payload_blob oid not null,
	reception_time timestamp not null,
	station_name varchar(255)
);

create table if not exists raw_station_data_frame_channel_names
(
	raw_station_data_frame_id uuid not null
		constraint fkd2dysxh3wkehh8jif7a3wkcm4
			references raw_station_data_frame ON DELETE CASCADE,
	channel_name varchar(255)
);
CREATE INDEX rsdf_channel_names_raw_station_data_frame_id_idx ON raw_station_data_frame_channel_names (raw_station_data_frame_id);

create table if not exists reference_alias
(
	id uuid not null
		constraint reference_alias_pkey
			primary key,
	actual_time timestamp,
	comment varchar(255),
	name varchar(255),
	status integer,
	system_time timestamp
);

create table if not exists reference_calibration
(
	id bigint not null
		constraint reference_calibration_pkey
			primary key,
	calibration_interval bigint
);

create table if not exists reference_calibration_calibrations
(
	calibration_id bigint
		constraint uk_fso90u61tdneo7jblgnadrd2n
			unique
		constraint fksf2jj07rcniw8q8716eq5a3rv
			references calibration,
	reference_calibration_id bigint not null
		constraint reference_calibration_calibrations_pkey
			primary key
		constraint fk3xriu5g5v7diglvqbb6fgai7x
			references reference_calibration
);

create table if not exists reference_channel
(
	id bigint not null
		constraint reference_channel_pkey
			primary key,
	actual_time timestamp,
	band_type varchar(255),
	comment varchar(255),
	data_type varchar(255),
	depth double precision,
	elevation double precision,
	entity_id uuid,
	horizontal_angle double precision,
	information_time timestamp not null,
	originating_organization varchar(255) not null,
	reference varchar(255) not null,
	instrument_type varchar(255),
	latitude double precision,
	location_code varchar(255),
	longitude double precision,
	name varchar(255),
	nominal_sample_rate double precision,
	orientation_code char,
	orientation_type varchar(255),
	east_displacement_km double precision not null,
	north_displacement_km double precision not null,
	vertical_displacement_km double precision not null,
	system_time timestamp,
	active boolean not null,
	units varchar(255),
	version_id uuid
		constraint uk_cpjva5lxy76sr7bi1vfrnn72d
			unique,
	vertical_angle double precision
);

create table if not exists reference_channel_aliases
(
	reference_channel bigint not null
		constraint fk8bq0pgo1ig69ww3hllbfxkb02
			references reference_channel,
	reference_alias uuid not null
		constraint uk_2yfd7375kwpqqt1waq1bxplus
			unique
		constraint fke0h8b4qyqivr3csoffmexfasr
			references reference_alias
);

create table if not exists reference_digitizer
(
	id bigint not null
		constraint reference_digitizer_pkey
			primary key,
	actual_time timestamp,
	comment varchar(255),
	description varchar(255),
	entity_id uuid,
	information_time timestamp not null,
	originating_organization varchar(255) not null,
	reference varchar(255) not null,
	manufacturer varchar(255),
	model varchar(255),
	name varchar(255),
	serial_number varchar(255),
	system_time timestamp,
	version_id uuid
		constraint uk_lvitx6uiejh7ed8k4828l51bu
			unique
);

create table if not exists reference_digitizer_membership
(
	primarykey bigint not null
		constraint reference_digitizer_membership_pkey
			primary key,
	actual_time timestamp,
	channel_id uuid,
	comment varchar(255),
	digitizer_id uuid,
	id uuid
		constraint uk_pfg6imcn0nfc7kylt03rpcm3y
			unique,
	status integer,
	system_time timestamp
);

create table if not exists reference_network
(
	id bigint not null
		constraint reference_network_pkey
			primary key,
	actual_time timestamp,
	comment varchar(255),
	description varchar(255),
	entity_id uuid,
	name varchar(255),
	org integer,
	region integer,
	information_time timestamp not null,
	originating_organization varchar(255) not null,
	reference varchar(255) not null,
	system_time timestamp,
	active boolean not null,
	version_id uuid
		constraint uk_c0mwh7ciqyisa73uwyqr7yp6d
			unique
);

create table if not exists reference_network_membership
(
	id uuid not null
		constraint reference_network_membership_pkey
			primary key,
	actual_time timestamp,
	comment varchar(255),
	network_id uuid,
	station_id uuid,
	status integer,
	system_time timestamp
);

create table if not exists reference_response
(
	id uuid not null
		constraint reference_response_pkey
			primary key,
	actual_time timestamp not null,
	channel_name varchar(255) not null,
	comment varchar(255),
	system_time timestamp not null
);

create table if not exists reference_response_frequency_amplitude_phase
(
	reference_frequency_amplitude_phase_id bigint
		constraint fkmih5x6k2uyfx94ban8mo9rpsj
			references frequency_amplitude_phase,
	reference_response_id uuid not null
		constraint reference_response_frequency_amplitude_phase_pkey
			primary key
		constraint fkk68g11989wf17rqrps58bntil
			references reference_response
);

create table if not exists reference_response_reference_calibrations
(
	reference_calibration_id bigint
		constraint fkc569ke3xpjn9o5bog057jbo2v
			references reference_calibration,
	reference_response_id uuid not null
		constraint reference_response_reference_calibrations_pkey
			primary key
		constraint fkr8amjtn7f2fafdr4ju81wker7
			references reference_response
);

create table if not exists reference_sensor
(
	id uuid not null
		constraint reference_sensor_pkey
			primary key,
	actual_time timestamp,
	channel_name varchar(255),
	comment varchar(255),
	corner_period double precision,
	high_passband double precision,
	information_time timestamp not null,
	originating_organization varchar(255) not null,
	reference varchar(255) not null,
	instrument_manufacturer varchar(255),
	instrument_model varchar(255),
	low_passband double precision,
	number_of_components integer,
	serial_number varchar(255),
	system_time timestamp
);

create table if not exists reference_site
(
	id bigint not null
		constraint reference_site_pkey
			primary key,
	actual_time timestamp,
	comment varchar(255),
	description varchar(255),
	elevation double precision,
	entity_id uuid,
	latitude double precision,
	longitude double precision,
	name varchar(255),
	east_displacement_km double precision not null,
	north_displacement_km double precision not null,
	vertical_displacement_km double precision not null,
	information_time timestamp not null,
	originating_organization varchar(255) not null,
	reference varchar(255) not null,
	system_time timestamp,
	active boolean not null,
	version_id uuid
		constraint uk_5196wdb3h5ruce4qkxgylpmf3
			unique
);

create table if not exists reference_site_aliases
(
	reference_site bigint not null
		constraint fkdfpjc0w7x23m6jkoyegv655ds
			references reference_site,
	reference_alias uuid not null
		constraint uk_oac82rneiqtcj788ratg0ygjl
			unique
		constraint fk63bhnh2g43q845e45abtcl5n5
			references reference_alias
);

create table if not exists reference_site_membership
(
	primarykey bigint not null
		constraint reference_site_membership_pkey
			primary key,
	actual_time timestamp,
	channel_name varchar(255),
	comment varchar(255),
	id uuid
		constraint uk_18yk8igpb537ykelf5gdup3j3
			unique,
	site_id uuid,
	status integer,
	system_time timestamp
);

create table if not exists reference_source_response
(
	id bigint not null
		constraint reference_source_response_pkey
			primary key,
	source_response_data bytea not null,
	source_response_type integer not null,
	source_response_units varchar(255) not null
);

create table if not exists reference_response_reference_source_response
(
	reference_source_response_id bigint
		constraint fkkfmlyafsvoyvimmayxpxgie3l
			references reference_source_response,
	reference_response_id uuid not null
		constraint reference_response_reference_source_response_pkey
			primary key
		constraint fkix8mjmei9y2cri3ce8ybldc8h
			references reference_response
);

create table if not exists reference_source_response_information_sources
(
	reference_source_response_id bigint not null
		constraint fknr7sa2hhxbappoe0ox9l5t0lg
			references reference_source_response,
	information_time timestamp not null,
	originating_organization varchar(255) not null,
	reference varchar(255) not null
);

create table if not exists reference_station
(
	id bigint not null
		constraint reference_station_pkey
			primary key,
	actual_time timestamp,
	comment varchar(255),
	description varchar(255),
	elevation double precision,
	entity_id uuid,
	latitude double precision,
	longitude double precision,
	name varchar(255),
	information_time timestamp not null,
	originating_organization varchar(255) not null,
	reference varchar(255) not null,
	station_type integer,
	system_time timestamp,
	active boolean not null,
	version_id uuid
		constraint uk_a4liqhl28yvqbql3wvk2hpkn
			unique
);

create table if not exists reference_station_aliases
(
	reference_station bigint not null
		constraint fk3i8o7osa8subv1yv9xosw2e20
			references reference_station,
	reference_alias uuid not null
		constraint uk_qdsbi43vmjpa1d96v0dsd2met
			unique
		constraint fkot5ntwxqsv31pguf8td7yywam
			references reference_alias
);

create table if not exists reference_station_membership
(
	id uuid not null
		constraint reference_station_membership_pkey
			primary key,
	actual_time timestamp,
	comment varchar(255),
	site_id uuid,
	station_id uuid,
	status integer,
	system_time timestamp
);

create sequence if not exists channel_configured_inputs_sequence increment by 5 minvalue 1 no maxvalue start with 1 no cycle;
create sequence if not exists reference_site_sequence increment by 5 minvalue 1 no maxvalue start with 1 no cycle;
create sequence if not exists reference_site_membership_sequence increment by 5 minvalue 1 no maxvalue start with 1 no cycle;
create sequence if not exists reference_network_sequence increment by 5 minvalue 1 no maxvalue start with 1 no cycle;
create sequence if not exists reference_channel_sequence increment by 5 minvalue 1 no maxvalue start with 1 no cycle;
create sequence if not exists reference_digitizer_sequence increment by 5 minvalue 1 no maxvalue start with 1 no cycle;
create sequence if not exists reference_calibration_sequence increment by 5 minvalue 1 no maxvalue start with 1 no cycle;
create sequence if not exists reference_station_sequence increment by 5 minvalue 1 no maxvalue start with 1 no cycle;
create sequence if not exists reference_source_response_sequence increment by 5 minvalue 1 no maxvalue start with 1 no cycle;
create sequence if not exists transferred_rsdf_sequence increment by 5 minvalue 1 no maxvalue start with 1 no cycle;
create sequence if not exists transferred_file_sequence increment by 5 minvalue 1 no maxvalue start with 1 no cycle;
create sequence if not exists transferred_file_invoice_sequence increment by 5 minvalue 1 no maxvalue start with 1 no cycle;
create sequence if not exists station_soh_issue_sequence increment by 5 minvalue 1 no maxvalue start with 1 no cycle;
create sequence if not exists calibration_sequence increment by 5 minvalue 1 no maxvalue start with 1 no cycle;
create sequence if not exists frequency_amplitude_phase_sequence increment by 5 minvalue 1 no maxvalue start with 1 no cycle;
create sequence if not exists waveform_summary_sequence increment by 5 minvalue 1 no maxvalue start with 1 no cycle;

create table if not exists response
(
	id uuid not null
		constraint response_pkey
			primary key,
	channel_name varchar(255)
		constraint uk_loshdqpmvdlilkl8nk4keikq2
			unique
		constraint fkhdo9c0el9dxe0wnn65x8v9bju
			references channel
);

create table if not exists response_calibrations
(
	calibration_id bigint
		constraint fkgrlpluuyqkg5lrvnky4hbubuy
			references calibration,
	response_id uuid not null
		constraint response_calibrations_pkey
			primary key
		constraint fkavf2r6e23hew4i0xqwt3jeoil
			references response
);

create table if not exists response_frequency_amplitude_phase
(
	frequency_amplitude_phase_id bigint
		constraint fkpg2cao0rbn1ifmcydw78l9p9r
			references frequency_amplitude_phase,
	response_id uuid not null
		constraint response_frequency_amplitude_phase_pkey
			primary key
		constraint fkktm28oscsjlc59r3r144y7ecb
			references response
);

create table if not exists soh_status
(
	id bigint not null
		constraint soh_status_pkey
			primary key,
	soh_status_env_status bigint
		constraint fkjagfdq7lccpdk5buuj19jm3us
			references environment_soh_status,
	soh_status_acq_status bigint not null
		constraint fk2qmod61yb5iqmk5kym7h8nww0
			references acquisition_soh_status
);

create table if not exists station
(
	name varchar(255) not null
		constraint station_pkey
			primary key,
	description varchar(1024) not null,
	depth double precision,
	elevation double precision,
	latitude double precision,
	longitude double precision,
	station_type varchar(255)
);

create table if not exists channel_group
(
	name varchar(255) not null
		constraint channel_group_pkey
			primary key,
	description varchar(255) not null,
	depth double precision,
	elevation double precision,
	latitude double precision,
	longitude double precision,
	type varchar(255),
	station_name varchar(255) not null
		constraint fklx0h0xrborbrx3m4d1qcvj2si
			references station
);

create index channel_group_station_idx on channel_group (station_name);

create table if not exists channel_group_channels
(
	channel_group_name varchar(255) not null
		constraint fksvjxwrjrcxlt2csn10a8vx1s3
			references channel_group,
	channel_name varchar(255) not null
		constraint fkpk8oa9rxtrfep8bftbje9usmk
			references channel
);

create index channel_group_channels_channel_group_idx on channel_group_channels (channel_group_name);
create index channel_group_channels_channel_idx on channel_group_channels (channel_name);

create table if not exists station_channel_info
(
	east_displacement_km double precision,
	north_displacement_km double precision,
	vertical_displacement_km double precision,
	channel_name varchar(255) not null
		constraint fk8jddgk6ss5b7kg9k9ys61vq6w
			references channel,
	station_name varchar(255) not null
		constraint fk42f4fhj6abkml9tre2dowbkl3
			references station,
	constraint station_channel_info_pkey
		primary key (channel_name, station_name)
);

create index station_channel_info_station_idx on station_channel_info (station_name);
create index station_channel_info_channel_idx on station_channel_info (channel_name);

create table if not exists station_group
(
	name varchar(255) not null
		constraint station_group_pkey
			primary key,
	description varchar(1024) not null
);

create table if not exists station_group_soh_status
(
	id uuid not null
		constraint station_group_soh_status_pkey
			primary key,
	end_time timestamp not null,
	soh_status_summary varchar(255) not null,
	start_time timestamp not null,
	station_group_name varchar(255) not null
		constraint station_goup_soh_status_station_group_fkey
			   references station_group 
);

create table if not exists station_group_stations
(
	station_group_name varchar(255) not null
		constraint fksn5vxm0l1o87ou6fakin0phqs
			references station_group,
	station_name varchar(255) not null
		constraint fklawgmjdptkps6pc4ah8c1cuov
			references station
);

create table if not exists station_soh_issue
(
	id bigint not null
		constraint station_soh_issue_pkey
			primary key,
	acknowledged_at timestamp,
	requires_acknowledgement boolean not null
);

create table if not exists station_soh_status
(
	id bigint not null
		constraint station_soh_status_pkey
			primary key,
	soh_status_summary varchar(255) not null,
	station_name varchar(255) not null,
	soh_status bigint not null
		constraint fk1p2v2840sjwjbltg3jjvvlpbi
			references soh_status,
	station_soh_issue bigint not null
		constraint fkhuj1sm4yvleb0fo9fbgnv0qlk
			references station_soh_issue,
	station_soh_status uuid
		constraint fkd13qfp7megyrfryiwfgm30ulv
			references station_group_soh_status
);

create table if not exists channel_soh_status
(
	id bigint not null
		constraint channel_soh_status_pkey
			primary key,
	channel_name varchar(255) not null
		constraint channel_soh_status_channel_fkey
			   references channel,
	soh_status bigint not null
		constraint fkhncc287qt2vhs9yccdl4gn88b
			references soh_status,
	channel_soh_status bigint
		constraint fkfp4jvfmyxp1v8j0ykuor50mmr
			references station_soh_status
);

create table if not exists transferred_file
(
	id bigint not null
		constraint transferred_file_pkey
			primary key,
	file_name varchar(255),
	metadata_type integer not null,
	priority varchar(255),
	reception_time timestamp,
	status varchar(255),
	transfer_time timestamp
);

create table if not exists transferred_file_invoice
(
	sequence_number bigint not null,
	id bigint not null
		constraint transferred_file_invoice_pkey
			primary key
		constraint fkoqoav67hkc04s21jrclhdl9ht
			references transferred_file
);

create table if not exists transferred_file_raw_station_data_frame
(
	payload_end_time timestamp not null,
	payload_start_time timestamp not null,
	station_name varchar(255),
	id bigint not null
		constraint transferred_file_raw_station_data_frame_pkey
			primary key
		constraint fk5dd4lkvjirm17c70d24fn0kxb
			references transferred_file
);

create table if not exists transferred_file_rsdf_metadata_channel_names
(
	channel_name bigint not null
		constraint fk8cxrw4j7s3wwx6w1mq7egj56a
			references transferred_file_raw_station_data_frame,
	channelnames varchar(255)
);

create table if not exists user_preferences
(
	id varchar(255) not null
		constraint user_preferences_pkey
			primary key,
	default_layout_name varchar(255),
	soh_layout_name varchar(255)
);

create table if not exists workspace_layout
(
	id uuid not null
	   constraint workspace_layout_pkey
	   	      primary key,
	layout_configuration text,
	name varchar(255),
	user_preferences_id varchar(255) not null
		constraint fkeenj804td36yjtle7mhdl2akn
			references user_preferences
);

create table if not exists audible_notification
(
	id uuid not null
	   constraint audible_notification_pkey
	   	      primary key,
    notification_type public.system_message_type_enum not null,
	file_name varchar(255) not null,
	user_preferences_id varchar(255) not null
	    constraint fk_user_prefs_notifications
	        references user_preferences
);

create table if not exists workspace_layout_supported_ui_modes
(
	workspace_layout_id uuid not null
		constraint fk_workspace_layout_ui_mode
			   references workspace_layout,
	supported_user_interface_mode varchar(255)
);			  

create table if not exists waveform_summary
(
	id bigint not null
		constraint waveform_summary_pkey
			primary key,
	channel_name varchar(255) not null,
	end_time timestamp not null,
	start_time timestamp not null,
	raw_station_data_frame_id uuid
		constraint fkm7fnlgi5cjf4mplnayqqy6vj
			references raw_station_data_frame ON DELETE CASCADE
);
CREATE INDEX waveform_summary_raw_station_data_frame_id_idx ON waveform_summary (raw_station_data_frame_id);
create index channel_name_end_time_idx on waveform_summary (channel_name, end_time);

-- new state of health objects (as part of the SOH capability guidance)
create sequence if not exists smvs_sequence increment by 10 minvalue 1 no maxvalue start with 1 no cycle;

create sequence if not exists station_soh_sequence increment by 5 minvalue 1 no maxvalue start with 1 no cycle;
create table if not exists station_soh(
	id int not null
	   	constraint station_soh_pkey
			primary key,
	coi_id uuid not null constraint coi_id_unique unique,
	creation_time timestamp not null,
	station_name varchar(255) not null
		constraint fk_station_soh_station
			references station(name),
	soh_status public.soh_status_enum not null
);
CREATE INDEX station_soh_station_name_idx ON station_soh (station_name);
CREATE INDEX station_soh_coi_id_idx ON station_soh (coi_id);

create sequence if not exists channel_soh_sequence increment by 5 minvalue 1 no maxvalue start with 1 no cycle;

create table if not exists channel_soh(
    id int not null
	   	constraint channel_soh_pkey
			primary key,
	channel_name varchar(255) not null
		constraint fk_channel_soh_channel_name
			references channel(name),			
	soh_status public.soh_status_enum not null,
	station_soh_id int
		constraint fk_station_soh_channel_soh
			references station_soh(id) ON DELETE CASCADE
);
CREATE INDEX channel_soh_channel_name_idx ON channel_soh (channel_name);
CREATE INDEX channel_soh_station_soh_id_idx ON channel_soh (station_soh_id);


create sequence if not exists station_aggregate_sequence increment by 50 minvalue 1 no maxvalue start with 1 no cycle;

create table if not exists station_aggregate(
    id bigint not null
      constraint station_aggregate_pkey
      primary key,
    station_soh_id int
    	constraint fk_station_soh_station_aggregate
    		references station_soh(id) ON DELETE CASCADE,    
    duration bigint,
    percent double precision,
    aggregate_type public.station_aggregate_type_enum not null,
    type varchar(255) not null
);
CREATE INDEX sa_station_soh_id_idx ON station_aggregate (station_soh_id);

create table if not exists soh_monitor_value_status(

    id int not null
	   	constraint soh_monitor_value_status_pkey
			primary key,
	duration int,
	percent real,
	channel_soh_id int
		constraint fk_channel_smvs_channel_soh
			references channel_soh(id) ON DELETE CASCADE,
	station_soh_id int
		constraint fk_station_soh_soh_monitor_rollup
			references station_soh(id) ON DELETE CASCADE,
	status smallint not null,
	monitor_type smallint not null
);
CREATE INDEX smvs_channel_soh_id_idx ON soh_monitor_value_status (channel_soh_id);
CREATE INDEX smvs_station_smvs_idx ON soh_monitor_value_status (station_soh_id);

create table if not exists soh_status_change_event(
	id uuid not null
    constraint soh_status_change_event_pkey primary key,
	station_name varchar(200) not null
	  constraint fk_soh_status_change_event_station
	    references station(name)
);
alter table soh_status_change_event
add constraint station_for_soh_status_change_event_unique UNIQUE(station_name);


create table if not exists soh_status_change_collection(
  unack_id uuid not null
    constraint fk_soh_status_change_collection_soh_status_change_event
      references soh_status_change_event(id) ON DELETE CASCADE,
  first_change_time timestamp not null,
  soh_monitor_type public.soh_monitor_type_enum not null,
  channel_name varchar(200) not null
    constraint fk_soh_status_change_collection_channel
    references channel(name)
);
alter table soh_status_change_collection
add constraint station_channel_change_time_monitor_type_unique UNIQUE(unack_id, first_change_time, soh_monitor_type, channel_name);

create table if not exists soh_status_change_quieted(
  soh_status_change_quieted_id uuid not null,
	quiet_until timestamp not null,
	quiet_duration bigint not null,
	soh_monitor_type public.soh_monitor_type_enum not null,
	comment varchar(1024),
  channel_name varchar(255) not null
		constraint fk_soh_status_change_quieted_channel
			references channel(name),
  station_name varchar(255) not null
    constraint fk_soh_status_change_quieted_station
      references station(name)
);

alter table soh_status_change_quieted
add constraint channel_name_station_monitor_type_unique UNIQUE(soh_monitor_type, channel_name);;

create table if not exists capability_soh_rollup(
  id uuid not null
    constraint capability_soh_rollup_pkey primary key,
  capability_rollup_time timestamp not null,
  group_rollup_status public.soh_status_enum not null,
  station_group_name varchar(255) not null
    constraint fk_capability_soh_rollup_station_group_rollup_status
      references station_group(name)
);

create table if not exists capability_station_soh_uuids(
  capability_rollup_id uuid not null
    constraint capability_station_soh_uuids_capability_soh_rollup
      references capability_soh_rollup(id),
  station_soh_id uuid not null
);

create table if not exists capability_station_soh_status_map(
  capability_rollup_id uuid not null
    constraint capability_station_soh_status_map_capability_soh_rollup
      references capability_soh_rollup(id),
  station_name varchar(255) not null
    constraint capability_station_soh_status_map_station
     references station(name),
   soh_status public.soh_status_enum not null
);

create table if not exists system_message(
    id uuid not null
     constraint system_message_pkey primary key,
    time timestamp not null,
    message text not null,
    system_message_type public.system_message_type_enum not null,
    system_message_severity public.system_message_severity_enum not null,
    system_message_category public.system_message_category_enum not null,
    system_message_sub_category public.system_message_sub_category_enum not null,
    messageTags jsonb
);

create or replace procedure public.delete_stale_records(acei_ttl_in_hours int, rsdf_ttl_in_hours int, ssoh_ttl_in_hours int)
language plpgsql as
$$
  declare
    num_analog_acei_deleted int;
    num_boolean_acei_deleted int;
    num_rsdfs_deleted int;
    num_station_soh_deleted int;

    analog_acei_elapsed_time double precision;
    boolean_acei_elapsed_time double precision;
    rsdf_elapsed_time double precision;
    ssoh_elapsed_time double precision;
    procedure_elapsed_time double precision;

    procedure_start_time timestamptz;
    procedure_end_time timestamptz;
    analog_acei_removal_start_time timestamptz;
    analog_acei_removal_end_time timestamptz;
    boolean_acei_removal_start_time timestamptz;
    boolean_acei_removal_end_time timestamptz;
    rsdf_removal_start_time timestamptz;
    rsdf_removal_end_time timestamptz;
    ssoh_removal_start_time timestamptz;
    ssoh_removal_end_time timestamptz;

    ttl_time timestamp;
  begin
    raise notice 'TTL worker started';

    procedure_start_time := clock_timestamp();

    ttl_time := now() - (acei_ttl_in_hours || ' hours')::interval;
    raise notice 'Deleting analog ACEI with end_time before %', ttl_time;
    analog_acei_removal_start_time := clock_timestamp();
    with analog_deleted as (delete from gms_soh.channel_env_issue_analog where end_time < ttl_time returning *) select count(*) from analog_deleted into num_analog_acei_deleted;
    analog_acei_removal_end_time := clock_timestamp();
    analog_acei_elapsed_time := 1000 * ( extract(epoch from analog_acei_removal_end_time) - extract(epoch from analog_acei_removal_start_time) );
    raise notice 'Deleted % analog ACEI in % ms', num_analog_acei_deleted, analog_acei_elapsed_time;

    raise notice 'Deleting boolean ACEI with end_time before %', ttl_time;
    boolean_acei_removal_start_time := clock_timestamp();
    with boolean_deleted as (delete from gms_soh.channel_env_issue_boolean where end_time < ttl_time returning *) select count(*) from boolean_deleted into num_boolean_acei_deleted;
    boolean_acei_removal_end_time := clock_timestamp();
    boolean_acei_elapsed_time := 1000 * ( extract(epoch from boolean_acei_removal_end_time) - extract(epoch from boolean_acei_removal_start_time) );
    raise notice 'Deleted % boolean ACEI in % ms', num_boolean_acei_deleted, boolean_acei_elapsed_time;

    ttl_time := now() - (rsdf_ttl_in_hours || ' hours')::interval;
    raise notice 'Deleting RSDFs with reception_time before %', ttl_time;
    rsdf_removal_start_time := clock_timestamp();
    with rsdfs_deleted as (delete from gms_soh.raw_station_data_frame where reception_time < ttl_time returning *) select count(*) from rsdfs_deleted into num_rsdfs_deleted;
    rsdf_removal_end_time := clock_timestamp();
    rsdf_elapsed_time := 1000 * ( extract(epoch from rsdf_removal_end_time) - extract(epoch from rsdf_removal_start_time) );
    raise notice 'Deleted % RSDFs in % ms', num_rsdfs_deleted, rsdf_elapsed_time;

    ttl_time := now() - (ssoh_ttl_in_hours || ' hours')::interval;
    raise notice 'Deleting Station SOH with creation_time before %', ttl_time;
    ssoh_removal_start_time := clock_timestamp();
    with station_soh_deleted as (delete from gms_soh.station_soh where creation_time < ttl_time returning *) select count(*) from station_soh_deleted into num_station_soh_deleted;
    ssoh_removal_end_time := clock_timestamp();
    ssoh_elapsed_time := 1000 * ( extract(epoch from ssoh_removal_end_time) - extract(epoch from ssoh_removal_start_time) );
    raise notice 'Deleted % Station SOH in % ms', num_station_soh_deleted, ssoh_elapsed_time;

    procedure_end_time := clock_timestamp();
    procedure_elapsed_time := 1000 * ( extract(epoch from procedure_end_time) - extract(epoch from procedure_start_time) );

    raise notice 'TTL worker finished in % ms', procedure_elapsed_time;
  end
$$;

-- Set gms_admin user for system to use with database
revoke all on schema gms_soh from gms_admin;
grant usage on schema gms_soh to gms_admin;
grant usage on sequence channel_configured_inputs_sequence to gms_admin;
grant usage on sequence reference_site_sequence to gms_admin;
grant usage on sequence reference_site_membership_sequence to gms_admin;
grant usage on sequence reference_network_sequence to gms_admin;
grant usage on sequence reference_channel_sequence to gms_admin;
grant usage on sequence reference_digitizer_sequence to gms_admin;
grant usage on sequence reference_calibration_sequence to gms_admin;
grant usage on sequence reference_station_sequence to gms_admin;
grant usage on sequence reference_source_response_sequence to gms_admin;
grant usage on sequence transferred_rsdf_sequence to gms_admin;
grant usage on sequence transferred_file_sequence to gms_admin;
grant usage on sequence transferred_file_invoice_sequence to gms_admin;
grant usage on sequence station_soh_issue_sequence to gms_admin;
grant usage on sequence calibration_sequence to gms_admin;
grant usage on sequence frequency_amplitude_phase_sequence to gms_admin;
grant usage on sequence waveform_summary_sequence to gms_admin;
grant select, insert, update, delete, truncate, references on all tables in schema gms_soh to gms_admin;
grant usage, select, update on all sequences in schema gms_soh to gms_admin;
revoke all on schema gms_soh_test from gms_admin;
grant usage on schema gms_soh_test to gms_admin;

-- TODO (sgk 1/24/2020) revisit testing user strategy
create role gms_soh_test_application with noinherit login encrypted password 'gmsdb:postgres:gms_soh_test:productive-dominancy-cordlessly';

revoke all on schema gms_soh from gms_soh_test_application;
grant all on schema gms_soh_test to gms_soh_test_application;

-- set up gms_soh_application user for hibernate to use to connect to the soh database
create role gms_soh_application with noinherit login encrypted password 'gmsdb:postgres:gms_soh_application:smidgeons-offerers-reducers';

grant usage on schema gms_soh to gms_soh_application;
grant usage on sequence channel_configured_inputs_sequence to gms_soh_application;
grant usage on sequence reference_site_sequence to gms_soh_application;
grant usage on sequence reference_site_membership_sequence to gms_soh_application;
grant usage on sequence reference_network_sequence to gms_soh_application;
grant usage on sequence reference_channel_sequence to gms_soh_application;
grant usage on sequence reference_digitizer_sequence to gms_soh_application;
grant usage on sequence reference_calibration_sequence to gms_soh_application;
grant usage on sequence reference_station_sequence to gms_soh_application;
grant usage on sequence reference_source_response_sequence to gms_soh_application;
grant usage on sequence transferred_rsdf_sequence to gms_soh_application;
grant usage on sequence transferred_file_sequence to gms_soh_application;
grant usage on sequence transferred_file_invoice_sequence to gms_soh_application;
grant usage on sequence station_soh_issue_sequence to gms_soh_application;
grant usage on sequence calibration_sequence to gms_soh_application;
grant usage on sequence frequency_amplitude_phase_sequence to gms_soh_application;
grant usage on sequence waveform_summary_sequence to gms_soh_application;
grant select, insert, update on all tables in schema gms_soh to gms_soh_application;
grant delete on capability_soh_rollup to gms_soh_application;
grant delete on capability_station_soh_uuids to gms_soh_application;
grant delete on capability_station_soh_status_map to gms_soh_application;
grant delete on channel_env_issue_boolean to gms_soh_application;
grant delete on channel_env_issue_analog to gms_soh_application;
grant delete on workspace_layout to gms_soh_application;
grant delete on audible_notification to gms_soh_application;
grant delete on workspace_layout_supported_ui_modes to gms_soh_application;
grant delete on station_group_stations to gms_soh_application;
grant delete on soh_status_change_event to gms_soh_application;
grant delete on soh_status_change_collection to gms_soh_application;

grant usage on sequence channel_configured_inputs_sequence to gms_soh_application;
grant usage on sequence reference_site_sequence to gms_soh_application;
grant usage on sequence reference_site_membership_sequence to gms_soh_application;
grant usage on sequence reference_network_sequence to gms_soh_application;
grant usage on sequence reference_channel_sequence to gms_soh_application;
grant usage on sequence reference_digitizer_sequence to gms_soh_application;
grant usage on sequence reference_calibration_sequence to gms_soh_application;
grant usage on sequence reference_station_sequence to gms_soh_application;
grant usage on sequence reference_source_response_sequence to gms_soh_application;
grant usage on sequence transferred_rsdf_sequence to gms_soh_application;
grant usage on sequence transferred_file_sequence to gms_soh_application;
grant usage on sequence transferred_file_invoice_sequence to gms_soh_application;
grant usage on sequence station_soh_issue_sequence to gms_soh_application;
grant usage on sequence calibration_sequence to gms_soh_application;
grant usage on sequence frequency_amplitude_phase_sequence to gms_soh_application;
grant usage on sequence waveform_summary_sequence to gms_soh_application;
grant usage on sequence smvs_sequence to gms_soh_application;
grant usage on sequence station_soh_sequence to gms_soh_application;	
grant usage on sequence channel_soh_sequence to gms_soh_application;
grant usage on sequence station_aggregate_sequence to gms_soh_application;
	
-- set up ttl user
create role gms_soh_ttl_application with noinherit login encrypted password 'gmsdb:postgres:gms_soh_ttl_application:ceilings-originate-eleventh';

revoke all on schema gms_soh from gms_soh_ttl_application;
grant usage on schema gms_soh to gms_soh_ttl_application;
grant select, delete, update on soh_monitor_value_status to gms_soh_ttl_application;
grant select, delete, update on channel_soh to gms_soh_ttl_application;
grant select, delete, update on station_aggregate to gms_soh_ttl_application;
grant select, delete, update on station_soh to gms_soh_ttl_application;
grant select, delete, update on soh_status_change_event to gms_soh_ttl_application;
grant select, delete, update on soh_status_change_quieted to gms_soh_ttl_application;
grant select, delete, update on soh_status_change_collection to gms_soh_ttl_application;
grant select, delete, update on channel_env_issue_analog to gms_soh_ttl_application;
grant select, delete, update on channel_env_issue_boolean to gms_soh_ttl_application;
grant select, delete, update on raw_station_data_frame to gms_soh_ttl_application;
grant select, delete, update on raw_station_data_frame_channel_names to gms_soh_ttl_application;
grant select, delete, update on capability_soh_rollup to gms_soh_ttl_application;
grant select, delete, update on capability_station_soh_uuids to gms_soh_ttl_application;
grant select, delete, update on capability_station_soh_status_map to gms_soh_ttl_application;





grant usage on sequence channel_configured_inputs_sequence to gms_soh_ttl_application;
grant usage on sequence frequency_amplitude_phase_sequence to gms_soh_ttl_application;	
grant usage on sequence reference_site_sequence to gms_soh_ttl_application;
grant usage on sequence reference_site_membership_sequence to gms_soh_ttl_application;
grant usage on sequence reference_network_sequence to gms_soh_ttl_application;
grant usage on sequence reference_channel_sequence to gms_soh_ttl_application;
grant usage on sequence reference_digitizer_sequence to gms_soh_ttl_application;
grant usage on sequence reference_calibration_sequence to gms_soh_ttl_application;
grant usage on sequence reference_station_sequence to gms_soh_ttl_application;
grant usage on sequence reference_source_response_sequence to gms_soh_ttl_application;
grant usage on sequence transferred_rsdf_sequence to gms_soh_ttl_application;
grant usage on sequence transferred_file_sequence to gms_soh_ttl_application;
grant usage on sequence transferred_file_invoice_sequence to gms_soh_ttl_application;
grant usage on sequence station_soh_issue_sequence to gms_soh_ttl_application;
grant usage on sequence calibration_sequence to gms_soh_ttl_application;
grant usage on sequence station_aggregate_sequence to gms_soh_ttl_application;
grant usage on sequence smvs_sequence to gms_soh_ttl_application;
grant usage on sequence channel_soh_sequence to gms_soh_ttl_application;
grant usage on sequence station_soh_sequence to gms_soh_ttl_application;
grant usage on sequence waveform_summary_sequence to gms_soh_ttl_application;
	
-- set up gms_read_only user for developers to use to connect to the database
revoke all on schema gms_soh from gms_read_only;
grant usage on schema gms_soh to gms_read_only;
grant select on all tables in schema gms_soh to gms_read_only;

-- Change ownership of all the tables to gms_admin

alter schema gms_soh owner to gms_admin;
alter sequence channel_configured_inputs_sequence owner to gms_admin;
alter sequence reference_site_sequence owner to gms_admin;
alter sequence reference_site_membership_sequence owner to gms_admin;
alter sequence reference_network_sequence owner to gms_admin;
alter sequence reference_channel_sequence owner to gms_admin;
alter sequence reference_digitizer_sequence owner to gms_admin;
alter sequence reference_calibration_sequence owner to gms_admin;
alter sequence reference_station_sequence owner to gms_admin;
alter sequence reference_source_response_sequence owner to gms_admin;
alter sequence transferred_rsdf_sequence owner to gms_admin;
alter sequence transferred_file_sequence owner to gms_admin;
alter sequence transferred_file_invoice_sequence owner to gms_admin;
alter sequence station_soh_issue_sequence owner to gms_admin;
alter sequence calibration_sequence owner to gms_admin;
alter sequence frequency_amplitude_phase_sequence owner to gms_admin;
alter sequence waveform_summary_sequence owner to gms_admin;
alter sequence channel_soh_sequence owner to gms_admin;
alter sequence smvs_sequence owner to gms_admin;
alter sequence station_aggregate_sequence owner to gms_admin;
alter sequence station_soh_sequence owner to gms_admin;
alter table acquisition_soh_status owner to gms_admin;
alter table calibration owner to gms_admin;
alter table channel owner to gms_admin;
alter table channel_configured_inputs owner to gms_admin;
alter table channel_env_issue_analog owner to gms_admin;
alter table channel_env_issue_boolean owner to gms_admin;
alter table environment_soh_status owner to gms_admin;
alter table environment_soh_counts_by_type owner to gms_admin;
alter table environment_soh_status_summaries owner to gms_admin;
alter table frequency_amplitude_phase owner to gms_admin;
alter table raw_station_data_frame owner to gms_admin;
alter table raw_station_data_frame_channel_names owner to gms_admin;
alter table reference_alias owner to gms_admin;
alter table reference_calibration owner to gms_admin;
alter table reference_calibration_calibrations owner to gms_admin;
alter table reference_channel owner to gms_admin;
alter table reference_channel_aliases owner to gms_admin;
alter table reference_digitizer owner to gms_admin;
alter table reference_digitizer_membership owner to gms_admin;
alter table reference_network owner to gms_admin;
alter table reference_network_membership owner to gms_admin;
alter table reference_response owner to gms_admin;
alter table reference_response_frequency_amplitude_phase owner to gms_admin;
alter table reference_response_reference_calibrations owner to gms_admin;
alter table reference_sensor owner to gms_admin;
alter table reference_site owner to gms_admin;
alter table reference_site_aliases owner to gms_admin;
alter table reference_site_membership owner to gms_admin;
alter table reference_source_response owner to gms_admin;
alter table reference_response_reference_source_response owner to gms_admin;
alter table reference_source_response_information_sources owner to gms_admin;
alter table reference_station owner to gms_admin;
alter table reference_station_aliases owner to gms_admin;
alter table reference_station_membership owner to gms_admin;
alter table response owner to gms_admin;
alter table response_calibrations owner to gms_admin;
alter table response_frequency_amplitude_phase owner to gms_admin;
alter table soh_status owner to gms_admin;
alter table station owner to gms_admin;
alter table channel_group owner to gms_admin;
alter table channel_group_channels owner to gms_admin;
alter table station_channel_info owner to gms_admin;
alter table soh_status_change_quieted owner to gms_admin;
alter table station_group owner to gms_admin;
alter table station_group_soh_status owner to gms_admin;
alter table station_group_stations owner to gms_admin;
alter table station_soh_issue owner to gms_admin;
alter table station_soh_status owner to gms_admin;
alter table channel_soh_status owner to gms_admin;
alter table transferred_file owner to gms_admin;
alter table transferred_file_invoice owner to gms_admin;
alter table transferred_file_raw_station_data_frame owner to gms_admin;
alter table transferred_file_rsdf_metadata_channel_names owner to gms_admin;
alter table user_preferences owner to gms_admin;
alter table workspace_layout owner to gms_admin;
alter table audible_notification owner to gms_admin;
alter table workspace_layout_supported_ui_modes owner to gms_admin;
alter table waveform_summary owner to gms_admin;
alter table channel_soh owner to gms_admin;
alter table soh_monitor_value_status owner to gms_admin;
alter table station_aggregate owner to gms_admin;
alter table station_soh owner to gms_admin;
alter table soh_status_change_event owner to gms_admin;
alter table soh_status_change_collection owner to gms_admin;
alter table capability_soh_rollup owner to gms_admin;
alter table capability_station_soh_uuids owner to gms_admin;
alter table capability_station_soh_status_map owner to gms_admin;
alter table system_message owner to gms_admin;

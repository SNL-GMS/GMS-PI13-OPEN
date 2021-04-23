import { SOHStationGroupNameWithPriority } from '@gms/common-graphql/lib/graphql/ui-configuration/types';
import { ChannelDataType, Location, Position, StationType, Units } from '../../common/model';

/**
 * Model definitions for the processing
 * StationGroup, Station, ChannelGroup and Channel data API
 */

/**
 * Encapsulates station-related data cached in memory
 */
export interface ProcessingStationData {
  stationGroupMap: Map<string, ProcessingStationGroup>;
  stationMap: Map<string, ProcessingStation>;
  channelGroupMap: Map<string, ProcessingChannelGroup>;
  channelMap: Map<string, ProcessingChannel>;
  sohStationGroupNameMap: Map<string, SOHStationGroupNameWithPriority[]>;
}
/**
 * Represents a group of stations used for monitoring.
 * This is the processing equivalent of the ReferenceNetwork.
 */
export interface ProcessingStationGroup {
  name: string;
  description: string;
  stations: ProcessingStation[];
}

/**
 * Represents an installation of monitoring sensors for the purposes of processing.
 * Multiple sensors can be installed at the same station.
 */
export interface ProcessingStation {
  name: string;
  type: StationType;
  description: string;
  relativePositionsByChannel: Map<string, Position>;
  location: Location;
  channelGroups: ProcessingChannelGroup[];
  channels: ProcessingChannel[];
}

/**
 * ChannelGroupType enum represents the different groupings of channels
 */
export enum ChannelGroupType {
  PROCESSING_GROUP = 'PROCESSING_GROUP',
  SITE_GROUP = 'SITE_GROUP'
}

/**
 * Represents a physical installation (e.g., building, underground vault, borehole)
 * containing a collection of Instruments that produce Raw Channel waveform data.
 */
export interface ProcessingChannelGroup {
  name: string;
  description: string;
  location: Location;
  type: ChannelGroupType;
  channels: ProcessingChannel[];
}

/**
 * Represents a source for unprocessed (raw) or processed (derived) time series data
 * from a seismic, hydroacoustic, or infrasonic sensor.
 */
export interface ProcessingChannel {
  name: string;
  canonicalName: string;
  description: string;
  station: string;
  channelDataType: ChannelDataType;
  channelBandType: ChannelBandType;
  channelInstrumentType: ChannelInstrumentType;
  channelOrientationType: ChannelOrientationType;
  channelOrientationCode: string;
  units: Units;
  nominalSampleRateHz: number;
  location: Location;
  orientationAngles: Orientation;
  configuredInputs: string[];
  processingDefinition: Map<string, any>;
  processingMetadata: Map<ChannelProcessingMetadataType, any>;
}

/**
 * Represents the orientation angles used in processing channels
 */
export interface Orientation {
  horizontalAngleDeg: number;
  verticalAngleDeg: number;
}

/**
 * Represents the type of processing metadata values that can appear as keys in the
 */
export enum ChannelProcessingMetadataType {
  // General properties
  CHANNEL_GROUP = 'CHANNEL_GROUP',

  // Filtering properties
  FILTER_CAUSALITY = 'FILTER_CAUSALITY',
  FILTER_GROUP_DELAY = 'FILTER_GROUP_DELAY',
  FILTER_HIGH_FREQUENCY_HZ = 'FILTER_HIGH_FREQUENCY_HZ',
  FILTER_LOW_FREQUENCY_HZ = 'FILTER_LOW_FREQUENCY_HZ',
  FILTER_PASS_BAND_TYPE = 'FILTER_PASS_BAND_TYPE',
  FILTER_TYPE = 'FILTER_TYPE',

  // Channel steering properties (used in beaming, rotation)
  STEERING_AZIMUTH = 'STEERING_AZIMUTH',
  STEERING_SLOWNESS = 'STEERING_SLOWNESS',

  // Beaming properties
  BEAM_COHERENT = 'BEAM_COHERENT'
}
/**
 * Represents the SEED / FDSN standard Channel Bands.  Each band has a corresponding single letter
 * code.
 */
export enum ChannelBandType {
  UNKNOWN = '-',

  // Long Period Bands
  MID_PERIOD = 'M', // 1Hz - 10Hz
  LONG_PERIOD = 'L', // ~1Hz
  VERY_LONG_PERIOD = 'V', // ~0.1Hz
  ULTRA_LONG_PERIOD = 'U', // ~0.01Hz
  EXTREMELY_LONG_PERIOD = 'R', // 0.0001Hz - 0.001Hz
  PARTICULARLY_LONG_PERIOD = 'P', // 0.00001Hz - 0.0001Hz (new)
  TREMENDOUSLY_LONG_PERIOD = 'T', // 0.000001Hz - 0.00001Hz (new)
  IMMENSELY_LONG_PERIOD = 'Q', // < 0.000001Hz (new)

  // Short Period Bands
  TREMENDOUSLY_SHORT_PERIOD = 'G', // 1000Hz - 5000Hz (new)
  PARTICULARLY_SHORT_PERIOD = 'D', // 250Hz - 10000Hz (new)
  EXTREMELY_SHORT_PERIOD = 'E', // 80Hz - 250Hz
  SHORT_PERIOD = 'S', // 10Hz - 80Hz

  // Broadband (Corner Periods > 10 sec)
  ULTRA_HIGH_BROADBAND = 'F', // 1000Hz - 5000Hz (new)
  VERY_HIGH_BROADBAND = 'C', // 250Hz - 1000Hz (new)
  HIGH_BROADBAND = 'H', // 80Hz - 250Hz
  BROADBAND = 'B', // 10Hz - 80Hz

  ADMINISTRATIVE = 'A',
  OPAQUE = 'O'
}

/**
 * Seismometer, Rotational Sensor, or Derived/Generated Orientations.
 * These correspond to instrument codes H, L, G, M, N, J, and X.
 */
export enum ChannelOrientationType {
  UNKNOWN = '-',
  VERTICAL = 'Z',
  NORTH_SOUTH = 'N',
  EAST_WEST = 'E',
  TRIAXIAL_A = 'A',
  TRIAXIAL_B = 'B',
  TRIAXIAL_C = 'C',
  TRANSVERSE = 'T',
  RADIAL = 'R',
  ORTHOGONAL_1 = '1',
  ORTHOGONAL_2 = '2',
  ORTHOGONAL_3 = '3',
  OPTIONAL_U = 'U',
  OPTIONAL_V = 'V',
  OPTIONAL_W = 'W'
}

/**
 * Represents the SEED / FDSN standard Channel Instruments.  Each instrument has a corresponding
 * single letter code.
 */
export enum ChannelInstrumentType {
  UNKNOWN = '-',
  HIGH_GAIN_SEISMOMETER = 'H',
  LOW_GAIN_SEISMOMETER = 'L',
  GRAVIMETER = 'G',
  MASS_POSITION_SEISMOMETER = 'M',
  ACCELEROMETER = 'N', // Historic channels might use L or G for accelerometers
  ROTATIONAL_SENSOR = 'J',
  TILT_METER = 'A',
  CREEP_METER = 'B',
  CALIBRATION_INPUT = 'C',
  PRESSURE = 'D',
  ELECTRONIC_TEST_POINT = 'E',
  MAGNETOMETER = 'F',
  HUMIDITY = 'I',
  TEMPERATURE = 'K',
  WATER_CURRENT = 'O',
  GEOPHONE = 'P',
  ELECTRIC_POTENTIAL = 'Q',
  RAINFALL = 'R',
  LINEAR_STRAIN = 'S',
  TIDE = 'T',
  BOLOMETER = 'U',
  VOLUMETRIC_STRAIN = 'V',
  WIND = 'W',
  NON_SPECIFIC_INSTRUMENT = 'Y',
  DERVIVED_BEAM = 'X',
  SYNTHESIZED_BEAM = 'Z'
}

import { QueryControls } from 'react-apollo';
import { CommonTypes } from '../';
import { StationType } from '../common/types';

// ***************************************
// Mutations
// ***************************************

// ***************************************
// Subscriptions
// ***************************************

// ***************************************
// Queries
// ***************************************

// tslint:disable-next-line:max-line-length interface-over-type-literal
export type DefaultStationsQueryProps = {
  defaultStationsQuery: QueryControls<{}> & { defaultReferenceStations: ReferenceStation[] };
};

// ***************************************
// Model
// ***************************************

/**
 * DataAcquistion used by the Data Acq UIs as status on stations
 * TODO: Should be broken out into own status and query
 */
export interface DataAcquisition {
  dataAcquisition: string;
  interactiveProcessing: string;
  automaticProcessing: string;
  acquisition: string;
  pkiStatus: string;
  pkiInUse: string;
  processingPartition: string;
  storeOnDataAcquisitionPartition: string;
}
/**
 * Reference Channel Definition
 */
export interface ReferenceChannel {
  id: string;
  name?: string;
  channelType: string;
  sampleRate: number;
  position?: CommonTypes.Position;
  actualTime?: string;
  systemTime?: string;
  depth?: number;
}

/**
 * Reference Site Definition
 */
export interface ReferenceSite {
  id: string;
  name?: string;
  channels: ReferenceChannel[];
  location: CommonTypes.Location;
}

export interface ReferenceStation {
  id: string;
  name?: string;
  stationType: StationType;
  description: string;
  defaultChannel: ReferenceChannel;
  networks: {
    id: string;
    name: string;
    monitoringOrganization: string;
  }[];
  modified: boolean;
  location: CommonTypes.Location;
  sites: ReferenceSite[];
  dataAcquisition: DataAcquisition;
  latitude: number;
  longitude: number;
  elevation: number;
}

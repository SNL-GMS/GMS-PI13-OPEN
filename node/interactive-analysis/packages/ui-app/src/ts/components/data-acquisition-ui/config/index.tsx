import { dataAcquisitionUserPreferences, DataAcquisitionUserPreferences } from './user-preferences';

export interface DataAcquisitionUIConfig {
  dataAcquisitionUserPreferences: DataAcquisitionUserPreferences;
}

export const dataAcquisitionUIConfig: DataAcquisitionUIConfig = {
  dataAcquisitionUserPreferences
};

export { dataAcquisitionUserPreferences } from './user-preferences';

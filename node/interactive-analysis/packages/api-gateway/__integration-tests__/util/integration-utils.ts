import { readJsonData } from '@gms/common-util';
import config from 'config';
import path from 'path';
import { resolveTestDataPaths } from '../../src/ts/util/test-data-util';

// tslint:disable: no-magic-numbers

// ASAR station id
export const STATION_IDS = ['1c4a8d11-178a-3c5d-a902-ac599a155621'];

// Channel ids for fk for MK01 and MK02
export const FK_CHANNEL_IDS = [
  '0d99cb69-425a-35ac-894c-9946637c5342',
  '0d8f3dd8-0f21-3558-9420-9198401de454'
];

// Channel segment ID for ASAR FKb
export const ASAR_FKB_CHANNEL_SEGMENT_ID = '9055926c-0f14-3b4e-8299-c79f3bc2f270';

// KDAK channel ids
export const KDAK_CHANNEL_IDS = [
  '991b0b6c-9708-3f63-98c4-b22ce06448f6',
  '842e91ea-4401-3d36-9a63-0fcc6da11c45'
];

// Event Id
export const EVENT_ID = 'a6bd4ad6-6224-309d-9cb2-866461abe679';

// Common Time range for testing
export const TIME_RANGE = {
  startTime: '2010-05-20T22:00:00Z',
  endTime: '2010-05-20T23:00:00Z',
  shortEndTime: '2010-05-20T22:05:00Z'
};

export const DEFAULT_USER = `"defaultUser"`;

/**
 * Get integration input from resources folder
 */
export function getIntegrationInput(integrationName: string) {
  const integrationPath = resolveTestDataPaths().integrationDataHome;
  const integrationInputs = config.get('testData.integrationInputs');
  return readJsonData(integrationPath.concat(path.sep).concat(integrationInputs[integrationName]));
}

import { readJsonData } from '@gms/common-util';
import config from 'config';
import { createDataPayload, createEmptyDataPayload } from '../../src/ts/util/common-utils';
import { resolveTestDataPaths } from '../../src/ts/util/test-data-util';

describe('common util payload helper function tests', () => {
  test('empty payload test', () => {
    const payload = createEmptyDataPayload();
    expect(payload).toBeDefined();
    expect(payload).toMatchSnapshot();
  });

  test('full payload test', () => {
    const paths = resolveTestDataPaths();
    const stdsFiles = config.get('testData.standardTestDataSet');
    const masks = [
      readJsonData(`${paths.jsonHome}/${stdsFiles.qcMask.qcMaskFileName}`).sort(m => m.id)[0]
    ];
    const events = [
      readJsonData(`${paths.jsonHome}/${stdsFiles.events.eventsFileName}`).sort(e => e.id)[0]
    ];
    const sds = [
      readJsonData(`${paths.jsonHome}/${stdsFiles.signalDetection.signalDetectionFileName}`).sort(
        sd => sd.id
      )[0]
    ];

    // Create Empty
    let payload = createDataPayload([], [], []);
    expect(payload).toBeDefined();
    expect(payload).toMatchSnapshot();

    // Create populated one
    payload = createDataPayload(events, sds, masks);
    expect(payload).toBeDefined();
    expect(payload.events.length).toEqual(1);
    expect(payload.sds.length).toEqual(1);
    expect(payload.qcMasks.length).toEqual(1);
  });
});

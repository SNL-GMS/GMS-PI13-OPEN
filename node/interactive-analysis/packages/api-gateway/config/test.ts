// tslint:disable-next-line: no-default-import
import defaultConfig from './default';

const testConfig = {
  ...defaultConfig,
  // New config name
  configName: 'test',
  // log level for testing is error only
  logLevel: 'error',
  // simulate waveforms
  waveform: {
    ...defaultConfig.waveform,
    waveformMode: 'simulated'
  },
  // change test data directory
  testData: {
    ...defaultConfig.testData,
    standardTestDataSet: {
      ...defaultConfig.testData.standardTestDataSet,
      stdsDataHome: 'resources/test_data/unit-test-data/Standard_Test_Data'
    }
  }
};

// tslint:disable-next-line: no-default-export
export default testConfig;

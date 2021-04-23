import { resolveHomeDataPath } from '@gms/common-util';
import config from 'config';
import path from 'path';
import { TestDataPaths } from '../common/model';
import { gatewayLogger as logger } from '../log/gateway-logger';

/**
 * Resolves the paths to the test data based of a yaml config
 * @returns Test data paths as TestDataPaths
 */
export function resolveTestDataPaths(): TestDataPaths {
  const testDataConfig = config.get('testData.standardTestDataSet');
  const dataHome = resolveHomeDataPath(testDataConfig.stdsDataHome)[0];
  const jsonHome = dataHome.concat(path.sep).concat(testDataConfig.stdsJsonDir);
  const fpHome = dataHome.concat(path.sep).concat(testDataConfig.featurePredictions);
  const fkHome = dataHome.concat(path.sep).concat(testDataConfig.fk.fkDataPath);
  const channelsHome = jsonHome
    .concat(path.sep)
    .concat(testDataConfig.channelSegment.channelSegmentSubDir);
  const additionalDataHome = config.get('testData.additionalTestData.dataPath');
  const integrationDataHome = config.get('testData.integrationInputs.dataPath');
  const tempTigerTeamData = config.get('testData.tempTigerTeamData.dataPath');

  logger.debug(`STDS Home:            ${dataHome}`);
  logger.debug(`STDS Jsons:           ${jsonHome}`);
  logger.debug(`STDS FP:              ${fpHome}`);
  logger.debug(`STDS Fk:              ${fkHome}`);
  logger.debug(`STDS Channel:         ${channelsHome}`);
  logger.debug(`Non-STDS Data:        ${additionalDataHome}`);
  logger.debug(`Integration Inputs:   ${integrationDataHome}`);

  return {
    dataHome,
    jsonHome,
    fpHome,
    fkHome,
    channelsHome,
    additionalDataHome,
    integrationDataHome,
    tempTigerTeamData
  };
}

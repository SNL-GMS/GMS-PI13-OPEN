import { getDurationMilliTime, getDurationTime } from '@gms/common-util';
import { UserContext } from '../cache/model';
import { getLogLevel } from '../log/gateway-logger';
import { ConfigurationProcessor } from './configuration-processor';
import { AnalystConfiguration, CommonConfiguration, SohConfiguration } from './model';

// GraphQL Resolvers
export const resolvers = {
  // Query resolvers
  Query: {
    uiAnalystConfiguration: async (_, __, userContext: UserContext) => {
      const logLevel = getLogLevel();
      const analystConfiguration: AnalystConfiguration = ConfigurationProcessor.Instance().getAnalystConfiguration();
      const sohConfiguration: SohConfiguration = ConfigurationProcessor.Instance().getSohConfiguration();
      const commonConfiguration: CommonConfiguration = ConfigurationProcessor.Instance().getCommonConfiguration();
      const sohParams = sohConfiguration.stationSohMonitoringDisplayParameters;

      return {
        logLevel,
        defaultNetwork: analystConfiguration.defaultNetwork,
        defaultFilters: analystConfiguration.defaultFilters,
        sohStationGroupNames: ConfigurationProcessor.Instance().getStationGroupNamesWithPriorities(),
        redisplayPeriod: getDurationMilliTime(sohParams.redisplayPeriod),
        reprocessingPeriod: getDurationTime(
          sohConfiguration.stationSohControlConfiguration.reprocessingPeriod
        ),
        acknowledgementQuietDuration: getDurationMilliTime(sohParams.acknowledgementQuietDuration),
        availableQuietDurations: sohParams.availableQuietDurations.map(getDurationMilliTime),
        sohStationStaleTimeMS: getDurationMilliTime(sohParams.sohStationStaleDuration),
        sohHistoricalDurations: sohParams.sohHistoricalDurations.map(getDurationMilliTime),
        systemMessageLimit: commonConfiguration.systemMessageLimit
        /* In future story modify the returned configuration to pass thru
           the analystConfiguration and sohConfiguration */
        // analystConfiguration,
        // sohConfiguration,
        // commonConfiguration
      };
    }
  }
};

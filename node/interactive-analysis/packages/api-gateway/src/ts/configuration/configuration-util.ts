import { SohTypes } from '@gms/common-graphql';

/**
 * Maps monitor type to value type
 * @param monitorType monitor type to map to value type
 */
export const getValueTypeForMonitor = (
  monitorType: SohTypes.SohMonitorType
): SohTypes.SohValueType =>
  monitorType === SohTypes.SohMonitorType.LAG
    ? SohTypes.SohValueType.DURATION
    : SohTypes.SohValueType.PERCENT;

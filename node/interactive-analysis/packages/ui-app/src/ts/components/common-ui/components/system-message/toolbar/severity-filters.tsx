import { SystemMessageTypes } from '@gms/common-graphql';
import { useForceUpdate } from '@gms/ui-util';
import Immutable from 'immutable';
import * as React from 'react';

export const buildDefaultSeverityFilterMap = () => {
  let newMap = Immutable.Map<SystemMessageTypes.SystemMessageSeverity, boolean>();
  // Initialize the severity filter map
  Object.keys(SystemMessageTypes.SystemMessageSeverity).forEach(severity => {
    newMap = newMap.set(SystemMessageTypes.SystemMessageSeverity[severity], true);
  });
  return newMap;
};

export const useSeverityFilters = (): [
  Immutable.Map<SystemMessageTypes.SystemMessageSeverity, boolean>,
  React.Dispatch<
    React.SetStateAction<Immutable.Map<SystemMessageTypes.SystemMessageSeverity, boolean>>
  >
] => {
  const [severityFilterMap, setSeverityFilterMap] = React.useState<
    Immutable.Map<SystemMessageTypes.SystemMessageSeverity, boolean>
  >(undefined);
  const forceUpdate = useForceUpdate();
  React.useEffect(() => {
    const timeoutHandle = setTimeout(forceUpdate);

    const newMap = buildDefaultSeverityFilterMap();
    setSeverityFilterMap(newMap);

    return () => clearTimeout(timeoutHandle);
  }, []);

  return [severityFilterMap, setSeverityFilterMap];
};

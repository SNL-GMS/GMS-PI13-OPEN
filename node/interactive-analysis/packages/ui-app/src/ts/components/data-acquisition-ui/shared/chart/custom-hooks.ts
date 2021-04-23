import { Domain } from '@gms/ui-core-components/lib/components/charts/types';
import React from 'react';

/**
 * A custom hook for managing the domain and zoom domain
 * of a chart.
 * @param startTime the initial start time
 * @param endTime the initial end time
 */
export const useDomain = (
  startTime: Date,
  endTime: Date
): [Domain, Domain, (d: Domain) => void] => {
  /** the entire (default) domain for the victory charts */
  const [domain, setDomain] = React.useState<Domain>({
    x: [startTime.getTime(), endTime.getTime()],
    y: [0, 1]
  });

  /** the zoom domain for the victory charts */
  const [zoomDomain, setZoomDomain] = React.useState<Domain>({
    x: [startTime.getTime(), endTime.getTime()],
    y: [0, 1]
  });

  React.useEffect(() => {
    setDomain({
      x: [startTime.getTime(), endTime.getTime()],
      y: [0, 1]
    });
    setZoomDomain({
      x: [startTime.getTime(), endTime.getTime()],
      y: [0, 1]
    });
  }, [startTime.getTime(), endTime.getTime()]);

  return [domain, zoomDomain, setZoomDomain];
};

import { MILLISECONDS_IN_SECOND, toDateTimeString } from '@gms/common-util';
import { LineChart } from '@gms/ui-core-components';
import { Domain } from '@gms/ui-core-components/lib/components/charts/types';
import * as React from 'react';

export interface ExternalAxisProps {
  domain: Domain;
  zoomDomain: Domain;
  onZoomDomainChange: React.Dispatch<React.SetStateAction<Domain>>;
  widthPx: number;
}

export const ExternalAxis: React.FunctionComponent<ExternalAxisProps> = props => (
  <LineChart
    id={'axis'}
    classNames={'external-axis'}
    domain={props.domain}
    zoomDomain={props.zoomDomain}
    onZoomDomainChange={props.onZoomDomainChange}
    lineDefs={[]}
    padding={{ top: 5, right: 70, bottom: 50, left: 80 }}
    // tslint:disable-next-line: no-magic-numbers
    heightPx={50}
    widthPx={props.widthPx}
    suppressYAxis={true}
    xAxisLabel={'Time'}
    xTickFormat={(timestamp: number) => toDateTimeString(timestamp / MILLISECONDS_IN_SECOND)}
  />
);

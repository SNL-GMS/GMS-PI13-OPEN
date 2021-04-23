import * as React from 'react';
import { QuietIndicator } from '~components/data-acquisition-ui/shared/quiet-indicator';
import { EnvironmentalSoh } from '../types';
import { QuietIndicatorWrapperProps } from './types';

const quietTimingInfoIsDefined = (quietData: EnvironmentalSoh) =>
  quietData &&
  quietData.quietTimingInfo &&
  quietData.quietTimingInfo.quietUntilMs &&
  quietData.quietTimingInfo.quietDurationMs;

/**
 * Renders a quiet indicator if the quietTimingInfo is all there.
 * Renders nothing if no quietUntilMs timestamp has been provided.
 */
export const MaybeQuietIndicator: React.FunctionComponent<QuietIndicatorWrapperProps> = props => {
  if (quietTimingInfoIsDefined(props.data)) {
    return (
      <QuietIndicator
        style={{ diameterPx: props.diameterPx }}
        status={props.data?.status?.toLowerCase()}
        quietTimingInfo={props.data.quietTimingInfo}
        className={props.className}
      />
    );
  }
  return null;
};

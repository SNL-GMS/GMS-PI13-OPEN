import { SohTypes } from '@gms/common-graphql';
import * as React from 'react';
import { messageConfig } from '~components/data-acquisition-ui/config/message-config';

// tslint:disable: no-require-imports no-var-requires
const badBadge = require('./resources/status-bad.svg');
const goodBadge = require('./resources/status-good.svg');
const marginalBadge = require('./resources/status-marginal.svg');
// tslint:enable: no-require-imports no-var-requires

const badgeTooltipMsg = messageConfig.tooltipMessages.stationStatistics.badge;

const badBadgeImage = (
  <img
    className="badge"
    width="17.583"
    height="23"
    src={badBadge}
    title={`${badgeTooltipMsg}BAD`}
  />
);
const goodBadgeImage = (
  <img className="badge" width="20" height="20" src={goodBadge} title={`${badgeTooltipMsg}GOOD`} />
);
const marginalBadgeImage = (
  <img
    className="badge"
    width="18.996"
    height="16.88"
    src={marginalBadge}
    title={`${badgeTooltipMsg}MARGINAL`}
  />
);

export interface WorstOfBadgeProps {
  worstOfSohStatus: SohTypes.SohStatusSummary;
  widthPx: number;
}

export interface WorstOfImageProps {
  worstOfSohStatus: SohTypes.SohStatusSummary;
}

export const WorstOfImage: React.FunctionComponent<WorstOfImageProps> = ({ worstOfSohStatus }) => {
  let badgeToUse;
  if (worstOfSohStatus === SohTypes.SohStatusSummary.BAD) {
    badgeToUse = badBadgeImage;
  } else if (worstOfSohStatus === SohTypes.SohStatusSummary.GOOD) {
    badgeToUse = goodBadgeImage;
  } else if (worstOfSohStatus === SohTypes.SohStatusSummary.MARGINAL) {
    badgeToUse = marginalBadgeImage;
  }
  return badgeToUse ?? null;
};

const shouldShowBadge = (worstOfSohStatus: SohTypes.SohStatusSummary) =>
  worstOfSohStatus === SohTypes.SohStatusSummary.BAD ||
  worstOfSohStatus === SohTypes.SohStatusSummary.MARGINAL;

/**
 * Selects which badge to use for group/station based on status. A status of "NONE" shows no badge.
 * @param worstOfSohStatus - the status of "GOOD," "BAD," or "MARGINAL"
 */
export const WorstOfBadge: React.FunctionComponent<WorstOfBadgeProps> = ({
  worstOfSohStatus,
  widthPx
}) =>
  shouldShowBadge(worstOfSohStatus) && (
    <div
      className={`soh-cell__right-container`}
      data-capability-status={worstOfSohStatus.toLowerCase()}
      style={{ width: widthPx }}
    >
      <WorstOfImage worstOfSohStatus={worstOfSohStatus} />
    </div>
  );

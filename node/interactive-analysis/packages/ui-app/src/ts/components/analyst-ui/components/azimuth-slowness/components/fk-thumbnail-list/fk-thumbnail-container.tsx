import { NonIdealState } from '@blueprintjs/core';
import { IconNames } from '@blueprintjs/icons';
import { EventTypes, SignalDetectionTypes, SignalDetectionUtils } from '@gms/common-graphql';
import React from 'react';
import { getFkData } from '~analyst-ui/common/utils/fk-utils';
import { FkUnits } from '../../types';
import { FkThumbnail } from '../fk-thumbnail';
import * as fkUtil from '../fk-util';

/**
 * Fk Thumbnail Props.
 */
export interface FkThumbnailContainerProps {
  data: SignalDetectionTypes.SignalDetection;
  signalDetectionFeaturePredictions: EventTypes.FeaturePrediction[];
  sizePx: number;
  selected: boolean;
  isUnassociated: boolean;
  fkUnit: FkUnits;
  arrivalTimeMovieSpectrumIndex: number;

  showFkThumbnailMenu?(x: number, y: number): void;
  onClick?(e: React.MouseEvent<HTMLDivElement>): void;
}

/**
 * A single fk thumbnail in the thumbnail-list
 */
export class FkThumbnailContainer extends React.Component<FkThumbnailContainerProps> {
  // ***************************************
  // BEGIN REACT COMPONENT LIFECYCLE METHODS
  // ***************************************

  /**
   * React component lifecycle.
   */
  public render() {
    const fmPhase = SignalDetectionUtils.findPhaseFeatureMeasurementValue(
      this.props.data.currentHypothesis.featureMeasurements
    );
    if (!this.props.data) {
      return <NonIdealState icon={IconNames.HEAT_GRID} title="All Fks Filtered Out" />;
    }
    const needsReview = fkUtil.fkNeedsReview(this.props.data);
    const label = `${this.props.data.stationName}` + ` ${fmPhase.phase.toString()}`;
    const fkData = getFkData(this.props.data.currentHypothesis.featureMeasurements);
    const arrivalTime: number = SignalDetectionUtils.findArrivalTimeFeatureMeasurementValue(
      this.props.data.currentHypothesis.featureMeasurements
    ).value;
    const predictedPoint = fkUtil.getPredictedPoint(this.props.signalDetectionFeaturePredictions);

    return (
      <FkThumbnail
        fkData={fkData}
        label={label}
        dimFk={this.props.isUnassociated}
        highlightLabel={needsReview}
        fkUnit={this.props.fkUnit}
        arrivalTime={arrivalTime}
        sizePx={this.props.sizePx}
        onClick={this.props.onClick}
        predictedPoint={predictedPoint}
        selected={this.props.selected}
        showFkThumbnailMenu={this.props.showFkThumbnailMenu}
        arrivalTimeMovieSpectrumIndex={this.props.arrivalTimeMovieSpectrumIndex}
      />
    );
  }
}

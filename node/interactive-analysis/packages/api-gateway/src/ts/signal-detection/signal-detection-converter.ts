import cloneDeep from 'lodash/cloneDeep';
import { convertFeatureMeasurementToOSD } from '../util/feature-measurement-convert-utils';
import { FeatureMeasurementTypeName, SignalDetection, SignalDetectionHypothesis } from './model';
import { SignalDetectionHypothesisOSD, SignalDetectionOSD } from './model-osd';

/**
 * Converts API Gateway SD to OSD format to send to OSD
 * @param sdToConvert Signal detection to be converted to osd format
 * @returns the signal detection in osd format
 */
export function convertSDtoOSD(sdToConvert: SignalDetection): SignalDetectionOSD {
  const sd = cloneDeep(sdToConvert);
  return {
    id: sd.id,
    monitoringOrganization: sd.monitoringOrganization,
    signalDetectionHypotheses: sd.signalDetectionHypotheses.map(
      convertSignalDetectionHypothesisToOSD
    ),
    stationName: sd.stationName
  };
}

/**
 * Converts a signal detection hypothesis to OSD format.
 * @param sdHyp signal detection hypothesis to convert
 * @returns the signal detection hypothesis in osd format
 */
export function convertSignalDetectionHypothesisToOSD(
  sdHyp: SignalDetectionHypothesis
): SignalDetectionHypothesisOSD {
  return {
    id: sdHyp.id,
    stationName: sdHyp.stationName,
    rejected: sdHyp.rejected,
    parentSignalDetectionId: sdHyp.parentSignalDetectionId,
    monitoringOrganization: sdHyp.monitoringOrganization,
    parentSignalDetectionHypothesisId: sdHyp.parentSignalDetectionHypothesisId,
    featureMeasurements: sdHyp.featureMeasurements
      .map(fm => {
        if (fm.featureMeasurementType !== FeatureMeasurementTypeName.FILTERED_BEAM) {
          return convertFeatureMeasurementToOSD(fm);
        }
      })
      .filter(fm => fm !== undefined)
  };
}

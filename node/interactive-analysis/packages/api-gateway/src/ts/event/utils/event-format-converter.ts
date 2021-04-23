import { getDurationTime, toEpochSeconds, toOSDTime } from '@gms/common-util';
import { produce } from 'immer';
import cloneDeep from 'lodash/cloneDeep';
import { UserContext } from '../../cache/model';
import { Units } from '../../common/model';
import { gatewayLogger as logger } from '../../log/gateway-logger';
import {
  FeatureMeasurementTypeName,
  FeatureMeasurementValue,
  InstantMeasurementValue,
  SignalDetection
} from '../../signal-detection/model';
import {
  InstantMeasurementValueOSD,
  NumericMeasurementValueOSD
} from '../../signal-detection/model-osd';
import { SignalDetectionProcessor } from '../../signal-detection/signal-detection-processor';
import { ProcessingStation } from '../../station/processing-station/model';
import { ProcessingStationProcessor } from '../../station/processing-station/processing-station-processor';
import {
  convertFeatureMeasurementFromOSD,
  convertFeatureMeasurementToOSD
} from '../../util/feature-measurement-convert-utils';
import * as model from '../model-and-schema/model';
import * as osdModel from '../model-and-schema/model-osd';
import {
  createLocationSolutionSet,
  findPreferredLocationSolutionSet,
  findPrefLocSolution,
  makeSignalDetectionSnapshots
} from './location-utils';

/**
 * Type of callback function for updating signal detections
 */
export type UpdateSignalDetectionsWithAssociationsCallback = () => void;

/**
 * Converts events from the osd format
 * @param eventOSD the osd event
 * @param currentStageId ID of the user's active stage
 * @param stations Default stations
 * @param allDetections all detections loaded into the system
 * @param getStationByChannelId Function that returns a station for the given channel id
 * @param validateFmId Callback that prints a warning if the given fm id is not in the system
 */
export function convertEventFromOSD(
  eventOSD: osdModel.EventOSD,
  currentStageId: string,
  stations: ProcessingStation[],
  allDetections: SignalDetection[],
  getStationByChannelId: (id: string) => ProcessingStation
): model.Event {
  const eventHyps = eventOSD.hypotheses.map(hyp =>
    convertEventHypothesisFromOSD(hyp, stations, allDetections, getStationByChannelId)
  );

  // hydrate final event list
  const finalEventHypothesisHistory = eventOSD.finalEventHypothesisHistory.map(feh =>
    eventHyps.find(eHyp => eHyp && eHyp.id === feh.eventHypothesisId)
  );
  let preferredEventHypothesisHistory: model.PreferredEventHypothesis[] = eventOSD.preferredEventHypothesisHistory
    .map(pEventHyp => {
      const eventHypothesis = eventHyps.find(eHyp => eHyp.id === pEventHyp.eventHypothesisId);
      return {
        eventHypothesis,
        processingStageId: pEventHyp.processingStageId
      };
      // TODO This was added to protect against a state where we saved an event
      // TODO - that was giving us an undefined event hyp in the preferred event hypothesis
      // TODO - upon further inspection this was an event with a conflict.
      // TODO We have an upcoming CR that will address conflicted event saves
      // TODO - this should be addressed then.
    })
    .filter(prefEventHyp => prefEventHyp.eventHypothesis !== undefined);

  // give it a status
  const status = model.EventStatus.ReadyForRefinement;
  const currentAndPreferred = getCurrentPreferredEventHypothesisAndHistory(
    preferredEventHypothesisHistory,
    currentStageId
  );
  preferredEventHypothesisHistory = currentAndPreferred.preferredEventHypothesisHistory;
  const currentEventHypothesis = currentAndPreferred.currentPrefEventHypo;
  const event: model.Event = {
    id: eventOSD.id,
    rejectedSignalDetectionAssociations: eventOSD.rejectedSignalDetectionAssociations,
    monitoringOrganization: eventOSD.monitoringOrganization,
    preferredEventHypothesisHistory,
    finalEventHypothesisHistory,
    hypotheses: eventHyps,
    status,
    currentEventHypothesis,
    hasConflict: false,
    associations: [],
    signalDetectionIds: []
  };
  return event;
}

/**
 * Convert an event to the osd format
 */
export function convertEventToOSD(userContext: UserContext, event: model.Event): osdModel.EventOSD {
  return {
    id: event.id,
    rejectedSignalDetectionAssociations: event.currentEventHypothesis.eventHypothesis.associations
      .filter(assoc => assoc.rejected)
      .map(assoc => assoc.id),
    monitoringOrganization: event.monitoringOrganization,
    hypotheses: event.hypotheses.map(hyp => convertEventHypothesisToOSD(userContext, hyp)),
    finalEventHypothesisHistory: event.finalEventHypothesisHistory
      ? event.finalEventHypothesisHistory.map(feh => ({
          eventHypothesisId: feh.id
        }))
      : undefined,
    preferredEventHypothesisHistory: event.preferredEventHypothesisHistory.map(pEHH => ({
      eventHypothesisId: pEHH.eventHypothesis.id,
      processingStageId: pEHH.processingStageId
    }))
  };
}

/**
 * Calculates the preferred event hypothesis for the stage
 * @param preferredEventHypothesisHistory existing history to pull the current entry from
 * @param currentStageId Stage being operated on by the user
 */
export function getCurrentPreferredEventHypothesisAndHistory(
  preferredEventHypothesisHistory: model.PreferredEventHypothesis[],
  currentStageId: string
): model.PreferredEventHypothesisHistoryAndHypothesis {
  // Lookup the preferred event for the current stage id.
  // If not found copy the previous stage's preferred (still todo for now copy last in list)
  let currentPrefEventHypo: model.PreferredEventHypothesis =
    preferredEventHypothesisHistory[preferredEventHypothesisHistory.length - 1];
  // If not set then not processing a new interval
  if (currentStageId) {
    const stagePrefHypothesisList: model.PreferredEventHypothesis[] = preferredEventHypothesisHistory.filter(
      preHypo => preHypo.processingStageId === currentStageId
    );
    const stagePrefHypothesis = stagePrefHypothesisList[stagePrefHypothesisList.length - 1];
    if (stagePrefHypothesis) {
      currentPrefEventHypo = stagePrefHypothesis;
    } else {
      currentPrefEventHypo = {
        ...currentPrefEventHypo,
        processingStageId: currentStageId
      };
      preferredEventHypothesisHistory.push(currentPrefEventHypo);
    }
  }
  return { preferredEventHypothesisHistory, currentPrefEventHypo };
}

/**
 * Sets the current event hypothesis to the top of the preferred event hyp history
 * @param event the event
 */
export function alignPreferredEventHypothesisHistoryWithHypothesis(
  event: model.Event
): model.Event {
  // Aligning preferred event hypothesis history and the hypotheses collection to refer to the same
  // event hypothesis object
  return produce<model.Event>(event, draftState => {
    if (
      draftState.preferredEventHypothesisHistory &&
      draftState.preferredEventHypothesisHistory.length > 0
    ) {
      draftState.preferredEventHypothesisHistory.forEach((prefEventHyp, index) => {
        const matchHyp = draftState.hypotheses.find(
          hyp => hyp.id === prefEventHyp.eventHypothesis.id
        );
        if (matchHyp) {
          draftState.preferredEventHypothesisHistory[index].eventHypothesis = matchHyp;
        } else {
          draftState.hypotheses.push(prefEventHyp.eventHypothesis);
        }
      });
    }
    draftState.currentEventHypothesis.eventHypothesis = draftState.hypotheses.find(
      hyp => hyp.id === draftState.currentEventHypothesis.eventHypothesis.id
    );
  });
}

/**
 * Converts location solution from the osd format
 * @param locationSolutionOSD the osd location solution
 * @param associations sd to event associations for the location's event
 * @param validateFmId callback that warns if the given fm id hasn't been loaded
 * @param getStationByChannelId callback to get stations by channel id
 * @param stations a list of default stations
 * @param allDetections all detections loaded into the system
 */
export function convertLocationSolutionFromOSD(
  locationSolutionOSD: osdModel.LocationSolutionOSD,
  associations: model.SignalDetectionEventAssociation[],
  stations: ProcessingStation[],
  allDetections: SignalDetection[]
): model.LocationSolution {
  const lbs = convertLocationBehaviorsFromOSD(locationSolutionOSD.locationBehaviors, allDetections);
  const sourceLocation: model.LocationSolution = {
    id: locationSolutionOSD.id,
    locationBehaviors: lbs,
    locationRestraint: locationSolutionOSD.locationRestraint,
    locationUncertainty: locationSolutionOSD.locationUncertainty,
    location: {
      ...locationSolutionOSD.location,
      time: toEpochSeconds(locationSolutionOSD.location.time)
    },
    featurePredictions: convertFeaturePredictionsFromOSD(locationSolutionOSD.featurePredictions),
    snapshots: makeSignalDetectionSnapshots(associations, lbs, allDetections, stations),
    // networkMagnitudeSolutions: []
    // hoping to convert received net mag sol's from osd
    networkMagnitudeSolutions: convertNetworkMagnitudeSolutionsFromOSD(
      locationSolutionOSD.networkMagnitudeSolutions
    )
  };
  return sourceLocation;
}

/**
 * Converts location solution to the osd format
 */
export function convertLocationSolutionToOSD(
  userContext: UserContext,
  locationSolution: model.LocationSolution
): osdModel.LocationSolutionOSD {
  const sourceLocationOSD: osdModel.LocationSolutionOSD = {
    id: locationSolution.id,
    locationRestraint: correctionForUnrestrainedForOSD(locationSolution.locationRestraint),
    locationUncertainty: locationSolution.locationUncertainty,
    locationBehaviors: convertLocationBehaviorsToOSD(
      userContext,
      locationSolution.locationBehaviors
    ),
    location: {
      ...locationSolution.location,
      time: toOSDTime(locationSolution.location.time)
    },
    featurePredictions: convertFeaturePredictionsToOSD(locationSolution.featurePredictions),
    networkMagnitudeSolutions: convertNetworkMagnitudeSolutionsToOSD(
      locationSolution.networkMagnitudeSolutions
    )
  };
  return sourceLocationOSD;
}

/**
 * Converts network magnitude behaviors to the osd format
 * @param networkMagnitudeSolutions the network magnitude solutions
 */
export function convertNetworkMagnitudeSolutionsToOSD(
  networkMagnitudeSolutions: model.NetworkMagnitudeSolution[]
): osdModel.NetworkMagnitudeSolutionOSD[] {
  return networkMagnitudeSolutions.map(nms => ({
    ...nms,
    magnitude: nms.magnitude ? nms.magnitude : 0,
    uncertainty: nms.uncertainty ? nms.uncertainty : 0,
    networkMagnitudeBehaviors: nms.networkMagnitudeBehaviors.map(nmb => ({
      ...nmb,
      stationMagnitudeSolution: {
        ...nmb.stationMagnitudeSolution,
        measurement: convertFeatureMeasurementToOSD(nmb.stationMagnitudeSolution.measurement)
      }
    }))
  }));
}

/**
 * Converts network magnitude behaviors from the osd format
 * @param networkMagnitudeSolutions the osd network magnitude solutions
 */
export function convertNetworkMagnitudeSolutionsFromOSD(
  networkMagnitudeSolutionsOSD: osdModel.NetworkMagnitudeSolutionOSD[]
): model.NetworkMagnitudeSolution[] {
  return networkMagnitudeSolutionsOSD.map(nmsOSD => ({
    ...nmsOSD,
    networkMagnitudeBehaviors: nmsOSD.networkMagnitudeBehaviors.map(nmb => ({
      ...nmb,
      stationMagnitudeSolution: {
        ...nmb.stationMagnitudeSolution,
        measurement: convertFeatureMeasurementFromOSD(nmb.stationMagnitudeSolution.measurement)
      }
    }))
  }));
}

/**
 * Converts feature predictions from the osd format
 * @param fpOSDs the osd feature predictions
 * @param getStationByChannelId Callback to get stations by channel id
 */
export function convertFeaturePredictionsFromOSD(
  fpOSDs: osdModel.FeaturePredictionOSD[]
): model.FeaturePrediction[] {
  if (!fpOSDs) {
    return [];
  }
  const fps: model.FeaturePrediction[] = fpOSDs
    .map(fpOSD => {
      // Check if the predicted value is populated.
      // If not create one (sometimes the COI streaming endpoints will return null)
      // TODO: As part of adding Az and Slow to FP need to figure out what default values (or reject) when not set
      let predictedValue: FeatureMeasurementValue;
      if (fpOSD.predictionType === FeatureMeasurementTypeName.ARRIVAL_TIME) {
        const predictedValueOSD: InstantMeasurementValueOSD = fpOSD.predictedValue as InstantMeasurementValueOSD;
        predictedValue = !predictedValueOSD
          ? createDefaultFeatureMeasurementValue(fpOSD.predictionType)
          : {
              value: predictedValueOSD.value ? toEpochSeconds(predictedValueOSD.value) : 0,
              standardDeviation: predictedValueOSD.standardDeviation
                ? // as unknown as string
                  getDurationTime(predictedValueOSD.standardDeviation)
                : 0
            };
      } else if (
        fpOSD.predictionType === FeatureMeasurementTypeName.SLOWNESS ||
        fpOSD.predictionType === FeatureMeasurementTypeName.RECEIVER_TO_SOURCE_AZIMUTH ||
        fpOSD.predictionType === FeatureMeasurementTypeName.SOURCE_TO_RECEIVER_AZIMUTH
      ) {
        const predictedValueOSD: NumericMeasurementValueOSD = fpOSD.predictedValue as NumericMeasurementValueOSD;
        predictedValue = !predictedValueOSD
          ? createDefaultFeatureMeasurementValue(fpOSD.predictionType)
          : {
              // as unknown as string
              referenceTime: toEpochSeconds((predictedValueOSD.referenceTime as unknown) as string),
              measurementValue: predictedValueOSD.measurementValue
            };
      } else {
        logger.warn(`Got unexpected feature measurement type of: ${fpOSD.predictionType}`);
      }

      // Create the Feature Prediction with converted from OSD
      const station = ProcessingStationProcessor.Instance().getStationByChannelName(
        fpOSD.channelName
      );
      const fp: model.FeaturePrediction = {
        ...fpOSD,
        stationName: station ? station.name : undefined,
        sourceLocation: {
          ...fpOSD.sourceLocation,
          time: toEpochSeconds(fpOSD.sourceLocation.time)
        },
        predictionType: fpOSD.predictionType,
        predictedValue
      };
      return fp;
    })
    .filter(fp => fp);
  if (!fps) {
    return [];
  }
  return fps;
}

/**
 * Converts location behaviors from the osd format
 * @param locationBehaviorsOSD the osd location behaviors
 * @param validateFmId callback that emits warning if given feature measurement id isn't cached
 */
export function convertLocationBehaviorsFromOSD(
  locationBehaviorsOSD: osdModel.LocationBehaviorOSD[],
  allDetections: SignalDetection[]
): model.LocationBehavior[] {
  return locationBehaviorsOSD
    .map(lbOSD => {
      const fm = convertFeatureMeasurementFromOSD(lbOSD.featureMeasurement);
      const signalDetection = getSignalDetectionByFeatureMeasurementId(
        fm.featureMeasurementType,
        fm.id,
        allDetections
      );
      if (!signalDetection) {
        logger.warn(
          `Failed to convert location behavior, will not be added, ` +
            `no Signal Detection found for fm id ${fm.id} number of sd reviewed: ${allDetections.length}`
        );
        return undefined;
      }
      const lb: model.LocationBehavior = {
        defining: lbOSD.defining,
        residual: isNaN(lbOSD.residual) ? -1 : lbOSD.residual,
        weight: isNaN(lbOSD.weight) ? -1 : lbOSD.weight,
        // featureMeasurementId: fm.id,
        featurePrediction: convertFeaturePredictionsFromOSD([lbOSD.featurePrediction])[0],
        featureMeasurementType: lbOSD.featureMeasurement.featureMeasurementType,
        signalDetectionId: signalDetection ? signalDetection.id : undefined
      };
      return lb;
    })
    .filter(lb => lb !== undefined);
}

/**
 * Converts location behaviors to the osd format
 */
export function convertLocationBehaviorsToOSD(
  userContext: UserContext,
  locationBehaviors: model.LocationBehavior[]
): osdModel.LocationBehaviorOSD[] {
  const lbOSDs: osdModel.LocationBehaviorOSD[] = locationBehaviors.map(lb => {
    const fmFromLocationBehavior = SignalDetectionProcessor.Instance().getFeatureMeasurementByIdAndType(
      userContext,
      lb.signalDetectionId,
      lb.featureMeasurementType
    );
    return {
      residual: lb.residual,
      weight: lb.weight,
      defining: lb.defining,
      featurePrediction: convertFeaturePredictionsToOSD([lb.featurePrediction])[0],
      featureMeasurement: fmFromLocationBehavior
        ? convertFeatureMeasurementToOSD(fmFromLocationBehavior)
        : undefined
    };
  });
  return lbOSDs;
}

/**
 * Forces any restraint value to be undefined if the restraint type is set to UNRESTRAINED
 * @param restraint location restraint object
 */
export function correctionForUnrestrainedForOSD(
  restraint: model.LocationRestraint
): model.LocationRestraint {
  return produce<model.LocationRestraint>(restraint, draftState => {
    if (draftState.depthRestraintType === model.DepthRestraintType.UNRESTRAINED) {
      draftState.depthRestraintKm = undefined;
    }

    if (draftState.latitudeRestraintType === model.RestraintType.UNRESTRAINED) {
      draftState.latitudeRestraintDegrees = undefined;
    }

    if (draftState.longitudeRestraintType === model.RestraintType.UNRESTRAINED) {
      draftState.longitudeRestraintDegrees = undefined;
    }

    if (draftState.timeRestraint === model.RestraintType.UNRESTRAINED) {
      draftState.timeRestraint = undefined;
    }
  });
}

/**
 * Creates a default Feature Measurement if none is returned from the service
 * @param predictionType FeatureMeasurementTypeName of feature prediction
 * @returns FeatureMeasurementValue based on the prediction type
 */
export function createDefaultFeatureMeasurementValue(
  predictionType: FeatureMeasurementTypeName
): FeatureMeasurementValue {
  let predictedValue: FeatureMeasurementValue;
  if (predictionType === FeatureMeasurementTypeName.ARRIVAL_TIME) {
    predictedValue = {
      value: 0,
      standardDeviation: 0
    };
  } else if (
    predictionType === FeatureMeasurementTypeName.SLOWNESS ||
    predictionType === FeatureMeasurementTypeName.RECEIVER_TO_SOURCE_AZIMUTH
  ) {
    predictedValue = {
      referenceTime: 0,
      measurementValue: {
        value: 0,
        standardDeviation: 0,
        units: Units.UNITLESS
      }
    };
  }
  return predictedValue;
}

/**
 * Convert the Feature Predictions being sent to the OSD
 * @param fps FeaturePrediction[] to be sent to OSD
 * @returns a converted FeaturePrediction[] -> FeaturePredictionOSD[]
 */
export function convertFeaturePredictionsToOSD(
  sourceFPs: model.FeaturePrediction[]
): osdModel.FeaturePredictionOSD[] {
  if (!sourceFPs || sourceFPs.length === 0) {
    return [];
  }
  const fps = cloneDeep(sourceFPs);
  const fpOSDs: osdModel.FeaturePredictionOSD[] = fps.map(fp => {
    // Station Name is not in OSD so remove it before spread operator
    delete fp.stationName;
    const newFP: osdModel.FeaturePredictionOSD = {
      ...fp,
      // receiverLocation: fp.receiverLocation,
      sourceLocation: {
        ...fp.sourceLocation,
        time: toOSDTime(fp.sourceLocation.time)
      },
      // predictionType: fp.predictionType,
      predictedValue: {
        ...fp.predictedValue,
        value: toOSDTime((fp.predictedValue as InstantMeasurementValue).value)
      },
      featurePredictionDerivativeMap: fp.featurePredictionDerivativeMap
        ? fp.featurePredictionDerivativeMap
        : {}
    };
    return newFP;
  });
  return fpOSDs;
}

/**
 * Convert the Event Hypothesis to an OSD compatible for sending as an argument
 * @param eventHypothesis to be sent to OSD
 * @param getFeatureMeasurementById callback that returns a feature measurement from an id
 * @returns a converted eventHypothesis -> eventHypothesisOSD
 */
export function convertEventHypothesisToOSD(
  userContext: UserContext,
  eventHypothesis: model.EventHypothesis
): osdModel.EventHypothesisOSD {
  // Only convert the LocationSolution Set that the Preferred Location Solution points to
  // one of it's members i.e. (Depth, Surface or Unrestrained)
  // Call the util to find the correct set
  const locationSolutionSet = findPreferredLocationSolutionSet(eventHypothesis);
  const locationSolutionsOSD: osdModel.LocationSolutionOSD[] = locationSolutionSet.locationSolutions.map(
    ls => convertLocationSolutionToOSD(userContext, ls)
  );
  const eventHypOSD: osdModel.EventHypothesisOSD = {
    id: eventHypothesis.id,
    rejected: eventHypothesis.rejected,
    eventId: eventHypothesis.eventId,
    parentEventHypotheses: eventHypothesis.parentEventHypotheses
      ? eventHypothesis.parentEventHypotheses
      : [],
    // TODO should rejected assocs be filtered out since they are already in a list at the event level
    associations: eventHypothesis.associations, // .filter(assoc => !assoc.rejected),
    locationSolutions: locationSolutionsOSD,
    preferredLocationSolution: {
      locationSolution: locationSolutionsOSD.find(
        ls => ls.id === eventHypothesis.preferredLocationSolution.locationSolution.id
      )
    }
  };
  return eventHypOSD;
}

/**
 * Converts event hypothesis from OSD to gateway version
 * @param eventHypothesisOSD the osd event hypothesis
 * @param stations default stations
 * @param allDetections a list of all detections in the system
 * @param validateFmId callback that validates a feature measurement id
 * @param getStationByChannelId callback that returns a station by channel ids
 */
export function convertEventHypothesisFromOSD(
  eventHypothesisOSD: osdModel.EventHypothesisOSD,
  stations: ProcessingStation[],
  allDetections: SignalDetection[],
  getStationByChannelId: (id: string) => ProcessingStation
): model.EventHypothesis {
  // Convert all the location solutions from OSD to API Gateway
  // Now set the Preferred to point at the correct (newly converted) Location Solution
  const prefLocSolutionId = eventHypothesisOSD.preferredLocationSolution.locationSolution.id;

  const eventHypothesis: model.EventHypothesis = {
    id: eventHypothesisOSD.id,
    rejected: eventHypothesisOSD.rejected,
    modified: false,
    eventId: eventHypothesisOSD.eventId,
    parentEventHypotheses: eventHypothesisOSD.parentEventHypotheses,
    associations: eventHypothesisOSD.associations.map(assoc => ({ ...assoc, modified: false })),
    locationSolutionSets: Object.seal([]),
    preferredLocationSolution: undefined
  };
  // update the location solution
  const locationSolutions = eventHypothesisOSD.locationSolutions.map(lsD =>
    convertLocationSolutionFromOSD(lsD, eventHypothesis.associations, stations, allDetections)
  );
  const updatedPreferredLocationSolution: model.PreferredLocationSolution = {
    locationSolution: findPrefLocSolution(prefLocSolutionId, locationSolutions)
  };
  const updatedLocationSolutionSet = createLocationSolutionSet(eventHypothesis, locationSolutions);

  const updatedEventHypothesis = produce<model.EventHypothesis>(eventHypothesis, draftState => {
    draftState.preferredLocationSolution = updatedPreferredLocationSolution;
    draftState.locationSolutionSets = Object.seal([updatedLocationSolutionSet]);
  });

  return updatedEventHypothesis;
}

/**
 * Finds the Signal Detection that contains the FM
 * @param fmType feature measurement type
 * @param fmId feature measurement id
 * @param allDetections all detection loaded into the cache
 *
 * @returns signal detection matching input feature measurement id
 */
function getSignalDetectionByFeatureMeasurementId(
  fmType: FeatureMeasurementTypeName,
  fmId: string,
  sds: SignalDetection[]
): SignalDetection {
  return sds.find(
    sd =>
      sd.currentHypothesis.featureMeasurements.find(
        fm => fm && fmId === fm.id && fmType === fm.featureMeasurementType
      ) !== undefined
  );
}

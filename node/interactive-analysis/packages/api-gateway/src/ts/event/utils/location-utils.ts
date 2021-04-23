import { uuid4 } from '@gms/common-util';
import produce from 'immer';
import { PhaseType } from '../../common/model';
import {
  FeatureMeasurement,
  FeatureMeasurementTypeName,
  SignalDetection,
  SignalDetectionHypothesis
} from '../../signal-detection/model';
import { ProcessingStation } from '../../station/processing-station/model';
import { mockBackendConfig } from '../../system-config';
import {
  getRandomLatitude,
  getRandomLatLonOffset,
  getRandomLongitude,
  getRandomResidual
} from '../../util/common-utils';
import {
  findAmplitudeFeatureMeasurementValue,
  findArrivalTimeFeatureMeasurement,
  findArrivalTimeFeatureMeasurementValue,
  findAzimuthFeatureMeasurement,
  findAzimuthFeatureMeasurementValue,
  findPhaseFeatureMeasurementValue,
  findSlownessFeatureMeasurement,
  findSlownessFeatureMeasurementValue
} from '../../util/feature-measurement-utils';
import {
  AmplitudeSnapshot,
  EventHypothesis,
  EventLocation,
  EventSignalDetectionAssociationValues,
  LocationBehavior,
  LocationSolution,
  LocationSolutionSet,
  SignalDetectionEventAssociation,
  SignalDetectionSnapshot
} from '../model-and-schema/model';
import { LocationBehaviorOSD } from '../model-and-schema/model-osd';

/**
 * Gets the location solution set with the highest count for the event hypothesis
 * @param eventHypothesis the event hypothesis
 */
export function getLatestLSSForEventHyp(eventHypothesis: EventHypothesis): LocationSolutionSet {
  return eventHypothesis.locationSolutionSets.reduce(
    (accum, val) => (val.count > accum.count ? val : accum),
    eventHypothesis.locationSolutionSets[0]
  );
}

/**
 * Find the list of location solutions pointed to by the Preferred Location Solution
 * @param eventHypothesis Which contains the Location Solution Sets (Surface, Depth, Unrestrained)
 * @returns A single set (list of Location Solutions) pointed to bye Preferred Location Solution
 */
export function findPreferredLocationSolutionSet(
  eventHypothesis: EventHypothesis
): LocationSolutionSet {
  const preferredLSId = eventHypothesis.preferredLocationSolution.locationSolution.id;
  let locationSolutionSet: LocationSolutionSet;
  eventHypothesis.locationSolutionSets.forEach(lsSet => {
    if (lsSet.locationSolutions.find(ls => ls.id === preferredLSId) !== undefined) {
      locationSolutionSet = lsSet;
    }
  });
  return locationSolutionSet;
}

/**
 * Find the Preferred Location Solution in EventHypothesis
 * @param eventHypothesis Which contains the Preferred Location Solution
 * @returns A single location solution pointed to by the preferred location solution id
 */
export function findPrefLocSolutionUsingEventHypo(
  eventHypothesis: EventHypothesis
): LocationSolution | undefined {
  if (!eventHypothesis) {
    return undefined;
  }
  const lsSet = findPreferredLocationSolutionSet(eventHypothesis);
  return findPrefLocSolution(
    eventHypothesis.preferredLocationSolution.locationSolution.id,
    lsSet.locationSolutions
  );
}

/**
 * Find the Preferred Location Solution in Location Solution Set
 * @param locationSolutionSet Which contains the Location Solution list of (Surface, Depth, Unrestrained)
 * @returns A single location solution pointed to by the preferred location solution id
 */
export function findPrefLocSolution(
  prefLocSolutionId: string,
  locSolutions: LocationSolution[]
): LocationSolution | undefined {
  if (!prefLocSolutionId || !locSolutions) {
    return undefined;
  }
  return locSolutions.find(ls => ls.id === prefLocSolutionId);
}

/**
 * Creates a location solution set
 * @param eventHyp event hypothesis
 * @param lsList list of location solutions
 * @return location solution set
 */
export function createLocationSolutionSet(
  eventHyp: EventHypothesis,
  lsList: LocationSolution[]
): LocationSolutionSet {
  return {
    id: uuid4(),
    count: eventHyp.locationSolutionSets.length,
    locationSolutions: lsList
  };
}

/**
 * Creates snapshots of the current sd associations for the passed in event hypothesis
 * This populates the location solution history used by the Event Location Solution UI
 * @param eventHypo event hypothesis
 * @returns snapshots of the associations
 */
export function makeSignalDetectionSnapshots(
  associations: SignalDetectionEventAssociation[],
  locationBehaviors: LocationBehavior[],
  allSignalDetections: SignalDetection[],
  stations: ProcessingStation[],
  convert?: boolean
): SignalDetectionSnapshot[] {
  const validAssociations = associations.filter(assoc => !assoc.rejected);
  const snapshots = validAssociations.map(sdAssoc => {
    const maybeSd = allSignalDetections.find(
      sd =>
        sd.signalDetectionHypotheses.find(sdh => sdh.id === sdAssoc.signalDetectionHypothesisId) !==
        undefined
    );
    if (maybeSd) {
      return createSignalDetectionSnapshot(locationBehaviors, sdAssoc, maybeSd);
    }
  });
  return snapshots.filter(snap => snap !== undefined && snap !== null);
}

/**
 * Creates snapshots of the current sd associations for the passed in event hypothesis
 * This populates the location solution history used by the Event Location Solution UI
 * @param eventHypo event hypothesis
 * @returns snapshots of the associations
 */
export function createSignalDetectionSnapshot(
  locationBehaviors: LocationBehavior[],
  sdAssoc: SignalDetectionEventAssociation,
  signalDetection: SignalDetection
): SignalDetectionSnapshot {
  const maybeHyp = signalDetection.signalDetectionHypotheses.find(
    sdh => sdh.id === sdAssoc.signalDetectionHypothesisId
  );
  if (!sdAssoc.rejected && maybeHyp) {
    const fms = maybeHyp.featureMeasurements;
    const slowFmVal = findSlownessFeatureMeasurementValue(fms);
    const azFmVal = findAzimuthFeatureMeasurementValue(fms);
    const arrivalFmVal = findArrivalTimeFeatureMeasurementValue(fms);
    const channelName = 'fkb';
    const maybePhase = findPhaseFeatureMeasurementValue(fms).phase
      ? findPhaseFeatureMeasurementValue(fms).phase
      : PhaseType.UNKNOWN;

    return {
      signalDetectionId: maybeHyp.parentSignalDetectionId,
      signalDetectionHypothesisId: maybeHyp.id,
      stationName: signalDetection.stationName,
      channelName,
      phase: maybePhase,
      time: getAssociationValues(
        maybeHyp.parentSignalDetectionId,
        FeatureMeasurementTypeName.ARRIVAL_TIME,
        fms,
        arrivalFmVal.value,
        locationBehaviors
      ),
      slowness: getAssociationValues(
        maybeHyp.parentSignalDetectionId,
        FeatureMeasurementTypeName.SLOWNESS,
        fms,
        slowFmVal.measurementValue.value,
        locationBehaviors
      ),
      azimuth: getAssociationValues(
        maybeHyp.parentSignalDetectionId,
        FeatureMeasurementTypeName.RECEIVER_TO_SOURCE_AZIMUTH,
        fms,
        azFmVal.measurementValue.value,
        locationBehaviors
      ),
      aFiveAmplitude: getAmplitudeSnapshot(
        signalDetection,
        FeatureMeasurementTypeName.AMPLITUDE_A5_OVER_2
      ),
      aLRAmplitude: getAmplitudeSnapshot(
        signalDetection,
        FeatureMeasurementTypeName.AMPLITUDE_ALR_OVER_2
      )
    };
  }
  return undefined;
}

/**
 * Gets a snapshot of the a-five amplitude measurement
 * @param sd Signal detection to get amplitude snapshot from
 */
export function getAmplitudeSnapshot(
  sd: SignalDetection,
  amplitudeName: FeatureMeasurementTypeName
): AmplitudeSnapshot {
  const maybeFm = findAmplitudeFeatureMeasurementValue(
    sd.currentHypothesis.featureMeasurements,
    amplitudeName
  );
  return maybeFm
    ? {
        amplitudeValue: maybeFm.amplitude.value,
        period: maybeFm.period
      }
    : undefined;
}

/**
 * Helper function to set the Azimuth, Slowness and Time values in the new SD Table Row
 * @param row LocationSDRow to populate
 * @param sd Signal Detection to find location behavior to populate from Slowness
 * @param locationBehaviors LocationBehaviors list from current selected event
 */
export function getAssociationValues(
  sdId: string,
  fmType: FeatureMeasurementTypeName,
  fms: FeatureMeasurement[],
  fmValue: number,
  locationBehaviors: LocationBehavior[]
): EventSignalDetectionAssociationValues {
  let fm: FeatureMeasurement;
  if (fmType === FeatureMeasurementTypeName.ARRIVAL_TIME) {
    fm = findArrivalTimeFeatureMeasurement(fms);
  } else if (fmType === FeatureMeasurementTypeName.SLOWNESS) {
    fm = findSlownessFeatureMeasurement(fms);
  } else if (
    fmType === FeatureMeasurementTypeName.SOURCE_TO_RECEIVER_AZIMUTH ||
    fmType === FeatureMeasurementTypeName.RECEIVER_TO_SOURCE_AZIMUTH
  ) {
    fm = findAzimuthFeatureMeasurement(fms);
  }
  const locBehavior = locationBehaviors.find(
    lb => lb.signalDetectionId === sdId && lb.featureMeasurementType === fm.featureMeasurementType
  );
  const maybeResidual = locBehavior && locBehavior.residual ? locBehavior.residual : -1;

  return {
    defining: locBehavior && locBehavior.defining ? locBehavior.defining : false,
    observed: fmValue,
    correction: undefined,
    residual: isNaN(maybeResidual) ? -1 : maybeResidual
  };
}

/**
 * Maps location behaviors and produces random residual values
 * @param locationBehaviors the location behaviors
 * @returns a location behavior[]
 */
export function randomizeResiduals(
  locationBehaviors: LocationBehaviorOSD[]
): LocationBehaviorOSD[] {
  return locationBehaviors.map(lb => ({
    ...lb,
    residual: getRandomResidual()
  }));
}

/**
 * sd
 */
export function getRandomLocationForNewHypothesis(
  sdHyps: SignalDetectionHypothesis[],
  startTime: number,
  allSds: SignalDetection[],
  stations: ProcessingStation[]
): EventLocation {
  let eventLocation: EventLocation;

  // If there are associations, the event location should be set to the earliest arriving SD
  if (sdHyps.length > 0) {
    const earliestArrival = sdHyps.reduce(
      (earliestSdHyp, sdHyp) =>
        findArrivalTimeFeatureMeasurementValue(sdHyp.featureMeasurements).value <
        findArrivalTimeFeatureMeasurementValue(earliestSdHyp.featureMeasurements).value
          ? sdHyp
          : earliestSdHyp,
      sdHyps[0]
    );
    // Copy the location from the station, and set event time as the arrival time
    const earliestArrivingSd = allSds.find(sd => sd.id === earliestArrival.parentSignalDetectionId);
    const station = stations.find(s => s.name === earliestArrivingSd.stationName);
    const arrivalTimeEpoch = findArrivalTimeFeatureMeasurementValue(
      earliestArrival.featureMeasurements
    ).value;
    const latLon = getRandomLatLonOffset();
    eventLocation = {
      latitudeDegrees: station.location.latitudeDegrees + latLon.lat,
      longitudeDegrees: station.location.longitudeDegrees + latLon.lon,
      depthKm: mockBackendConfig.defaultEventDepth,
      time: arrivalTimeEpoch - mockBackendConfig.defaultOffsetForEventLocation
    };
  } else {
    // No associations found - randomize the lat/lon and set time to the start of the interval
    eventLocation = {
      latitudeDegrees: getRandomLatitude(),
      longitudeDegrees: getRandomLongitude(),
      depthKm: mockBackendConfig.defaultEventDepth,
      time: startTime + 1
    };
  }
  return eventLocation;
}

/**
 * Returns a clone of the event hypothesis with the updated preferred location solution
 * @param preferredLocationSolutionId the preferred location solution id
 * @param eventHypo the event hypothesis
 */
export function produceEventHypothesisWithCorrectPreferredLocationSolution(
  preferredLocationSolutionId: string,
  eventHypo: EventHypothesis
): EventHypothesis {
  // Update event hypo with location solution behaviors preferred location solution
  return produce<EventHypothesis>(eventHypo, draftState => {
    if (
      preferredLocationSolutionId &&
      eventHypo.preferredLocationSolution.locationSolution.id !== preferredLocationSolutionId
    ) {
      const preferredLocationSolution = findPrefLocSolutionUsingEventHypo(eventHypo);
      if (preferredLocationSolution) {
        draftState.preferredLocationSolution.locationSolution = preferredLocationSolution;
        draftState.preferredLocationSolution = {
          ...eventHypo.preferredLocationSolution,
          locationSolution: preferredLocationSolution
        };
      }
    }
  });
}

/**
 * Modifies an event hypothesis so that it is in the right format for a location call
 * @param eventHyp The hypothesis to modify
 * @param preferredLocationSolutionId the id of the location solution to use
 * @param locationBehaviors the location behaviors to use in the locate call
 */
export function produceEventHypothesisForLocationCall(
  eventHyp: EventHypothesis,
  preferredLocationSolutionId: string,
  locationBehaviors: LocationBehavior[]
): EventHypothesis {
  const hypToUse = produceEventHypothesisWithCorrectPreferredLocationSolution(
    preferredLocationSolutionId,
    eventHyp
  );
  // Clone the eventHyp so the data changed as the input argument is not reflected in the event cache
  const correctedHypothesis: EventHypothesis = produce<EventHypothesis>(hypToUse, draftState => {
    // Update the preferred location solution location behaviors and remove all but preferred
    // location solution from the location solution list. We will need this when we send
    // the three location solutions (surface, depth and unrestrained) not just the one
    draftState.preferredLocationSolution.locationSolution.locationBehaviors = locationBehaviors;
    draftState.locationSolutionSets = [findPreferredLocationSolutionSet(hypToUse)];
  });
  return correctedHypothesis;
}

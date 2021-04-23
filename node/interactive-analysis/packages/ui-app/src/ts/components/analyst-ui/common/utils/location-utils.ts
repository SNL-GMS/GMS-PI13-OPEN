import { EventTypes, SignalDetectionTypes, SignalDetectionUtils } from '@gms/common-graphql';
import cloneDeep from 'lodash/cloneDeep';
import {
  DefiningChange,
  DefiningTypes,
  LocationSDRow
} from '~analyst-ui/components/location/components/location-signal-detections/types';
import {
  DefiningStatus,
  SignalDetectionSnapshotWithDiffs,
  SignalDetectionTableRowChanges
} from '~analyst-ui/components/location/types';
import { gmsColors } from '~scss-config/color-preferences';
import { getLatestLocationSolutionSet } from './event-util';

/**
 * Helper function to lookup the location behavior in the event's location behavior list
 * that corresponds to that Feature Measurement Id
 *
 * @param locationBehaviors Event's Location Behaviors list
 * @param definingType which isDefining value to update Arrival Time, Slowness or Azimuth
 * @param sd SignalDetection which has the FeatureMeasurements to search
 *
 * @returns LocationBehavior
 */
export function getLocationBehavior(
  definingType: DefiningTypes,
  sd: SignalDetectionTypes.SignalDetection,
  locationBehaviors: EventTypes.LocationBehavior[]
): EventTypes.LocationBehavior {
  const fmt: SignalDetectionTypes.FeatureMeasurementTypeName =
    definingType === DefiningTypes.AZIMUTH
      ? SignalDetectionTypes.FeatureMeasurementTypeName.RECEIVER_TO_SOURCE_AZIMUTH
      : definingType === DefiningTypes.ARRIVAL_TIME
      ? SignalDetectionTypes.FeatureMeasurementTypeName.ARRIVAL_TIME
      : definingType === DefiningTypes.SLOWNESS
      ? SignalDetectionTypes.FeatureMeasurementTypeName.SLOWNESS
      : undefined;
  return locationBehaviors.find(
    lb => lb.signalDetectionId === sd.id && lb.featureMeasurementType === fmt
  );
}

/**
 * Gets the defining settings for an sd based on a new set of defining
 * @param definingType Type of location behavior to change
 * @param setDefining Whether defining will change to true or false
 * @param signalDetection The signal detection being affected
 * @param sdRowChanges The current state of defining settings in the ui
 * @param openEvent The open event
 */
export function getNewDefiningForSD(
  definingType: DefiningTypes,
  setDefining: boolean,
  signalDetection: SignalDetectionTypes.SignalDetection,
  sdRowChanges: SignalDetectionTableRowChanges,
  openEvent: EventTypes.Event
) {
  const locationBehaviors =
    openEvent.currentEventHypothesis.eventHypothesis.preferredLocationSolution.locationSolution
      .locationBehaviors;

  const originalAzimuthDefining = getLocationBehavior(
    DefiningTypes.AZIMUTH,
    signalDetection,
    locationBehaviors
  )
    ? getLocationBehavior(DefiningTypes.AZIMUTH, signalDetection, locationBehaviors).defining
    : false;
  const originalArrivalTimeDefining = getLocationBehavior(
    DefiningTypes.ARRIVAL_TIME,
    signalDetection,
    locationBehaviors
  )
    ? getLocationBehavior(DefiningTypes.ARRIVAL_TIME, signalDetection, locationBehaviors).defining
    : false;
  const originalSlownessDefining = getLocationBehavior(
    DefiningTypes.SLOWNESS,
    signalDetection,
    locationBehaviors
  )
    ? getLocationBehavior(DefiningTypes.SLOWNESS, signalDetection, locationBehaviors).defining
    : false;
  const newSdRowChanges: SignalDetectionTableRowChanges = {
    arrivalTimeDefining:
      definingType === DefiningTypes.ARRIVAL_TIME
        ? setDefining !== originalArrivalTimeDefining
          ? setDefining
            ? DefiningChange.CHANGED_TO_TRUE
            : DefiningChange.CHANGED_TO_FALSE
          : DefiningChange.NO_CHANGE
        : sdRowChanges.arrivalTimeDefining,
    azimuthDefining:
      definingType === DefiningTypes.AZIMUTH
        ? setDefining !== originalAzimuthDefining
          ? setDefining
            ? DefiningChange.CHANGED_TO_TRUE
            : DefiningChange.CHANGED_TO_FALSE
          : DefiningChange.NO_CHANGE
        : sdRowChanges.azimuthDefining,
    slownessDefining:
      definingType === DefiningTypes.SLOWNESS
        ? setDefining !== originalSlownessDefining
          ? setDefining
            ? DefiningChange.CHANGED_TO_TRUE
            : DefiningChange.CHANGED_TO_FALSE
          : DefiningChange.NO_CHANGE
        : sdRowChanges.slownessDefining,
    signalDetectionId: signalDetection.id
  };
  return newSdRowChanges;
}

/**
 * Helper function to set the Time values in the new SD Table Row
 * @param row LocationSDRow to populate
 * @param sd Signal Detection to find location behavior to populate from Slowness
 * @param locationBehaviors LocationBehavors list from current selected event
 */
export function getArrivalTimeValues(
  sd: SignalDetectionTypes.SignalDetection,
  locationBehaviors: EventTypes.LocationBehavior[]
): { obs: number; res: number } {
  const fmValue = SignalDetectionUtils.findArrivalTimeFeatureMeasurementValue(
    sd.currentHypothesis.featureMeasurements
  );
  const locBehavior = getLocationBehavior(DefiningTypes.ARRIVAL_TIME, sd, locationBehaviors);
  return {
    obs: fmValue.value,
    res: locBehavior ? locBehavior.residual : undefined
  };
}

/**
 * Helper function to set the Azimiuth values in the new SD Table Row
 * @param row LocationSDRow to populate
 * @param sd Signal Detection to find location behavior to populate from Slowness
 * @param locationBehaviors LocationBehavors list from current selected event
 */
export function getAzimuthValues(
  sd: SignalDetectionTypes.SignalDetection,
  locationBehaviors: EventTypes.LocationBehavior[]
): { obs: number; res: number } {
  const fmValue = SignalDetectionUtils.findAzimuthFeatureMeasurementValue(
    sd.currentHypothesis.featureMeasurements
  );
  const locBehavior = getLocationBehavior(DefiningTypes.AZIMUTH, sd, locationBehaviors);
  return {
    obs: fmValue ? fmValue.measurementValue.value : undefined,
    res: locBehavior ? locBehavior.residual : undefined
  };
}

/**
 * Helper function to set the Slowness values in the new SD Table Row
 * @param row LocationSDRow to populate
 * @param sd Signal Detection to find location behavior to populate from Slowness
 * @param locationBehaviors LocationBehavors list from current selected event
 */
export function getSlownessValues(
  sd: SignalDetectionTypes.SignalDetection,
  locationBehaviors: EventTypes.LocationBehavior[]
): { obs: number; res: number } {
  const fmValue = SignalDetectionUtils.findSlownessFeatureMeasurementValue(
    sd.currentHypothesis.featureMeasurements
  );
  const locBehavior = getLocationBehavior(DefiningTypes.SLOWNESS, sd, locationBehaviors);
  return {
    obs: fmValue ? fmValue.measurementValue.value : undefined,
    res: locBehavior ? locBehavior.residual : undefined
  };
}

/**
 * Gets the proper cell style for a dif
 * @param definingType Type of cell to mark
 * @param row params from ag grid
 */
export function getDiffStyleForDefining(definingType: DefiningTypes, row: LocationSDRow): any {
  switch (definingType) {
    case DefiningTypes.ARRIVAL_TIME:
      return {
        'background-color': row.timeDefiningDiff ? gmsColors.gmsTableChangeMarker : ''
      };
      break;
    case DefiningTypes.AZIMUTH:
      return {
        'background-color': row.azimuthDefiningDiff ? gmsColors.gmsTableChangeMarker : ''
      };
      break;
    case DefiningTypes.SLOWNESS:
      return {
        'background-color': row.slownessDefiningDiff ? gmsColors.gmsTableChangeMarker : ''
      };
      break;
    default:
      return {};
  }
}
/**
 * Gets the channel name for an sd
 * @param signalDetection Signal detection to get channel name from
 */
export function getChannelName(signalDetection: SignalDetectionTypes.SignalDetection): string {
  const maybeArrivalTime = signalDetection.currentHypothesis.featureMeasurements.find(
    fm => fm.featureMeasurementType === SignalDetectionTypes.FeatureMeasurementTypeName.ARRIVAL_TIME
  );
  if (maybeArrivalTime) {
    if (maybeArrivalTime.channelSegment) {
      return maybeArrivalTime.channelSegment.name;
    }
  }
  return '';
}

/**
 *
 * @param sd Signal detection to convert
 * @param locationBehaviors  location behaviors for the sd as sent fromt the gateway
 * @param sdDefiningFromTable defining states for the sd as sent from the location sd table
 */
export function convertSignalDetectionToSnapshotWithDiffs(
  sd: SignalDetectionTypes.SignalDetection,
  locationBehaviors: EventTypes.LocationBehavior[],
  sdDefiningFromTable: DefiningStatus | undefined
): SignalDetectionSnapshotWithDiffs {
  const arrivalTimeValues = getArrivalTimeValues(sd, locationBehaviors);
  const slownessValues = getSlownessValues(sd, locationBehaviors);
  const azimuthValues = getAzimuthValues(sd, locationBehaviors);
  const isDefiningTime =
    sdDefiningFromTable.arrivalTimeDefining === DefiningChange.CHANGED_TO_FALSE
      ? false
      : sdDefiningFromTable.arrivalTimeDefining === DefiningChange.CHANGED_TO_TRUE
      ? true
      : getLocationBehavior(DefiningTypes.ARRIVAL_TIME, sd, locationBehaviors)
      ? getLocationBehavior(DefiningTypes.ARRIVAL_TIME, sd, locationBehaviors).defining
      : false;
  const isDefiningAzimuth =
    sdDefiningFromTable.azimuthDefining === DefiningChange.CHANGED_TO_FALSE
      ? false
      : sdDefiningFromTable.azimuthDefining === DefiningChange.CHANGED_TO_TRUE
      ? true
      : getLocationBehavior(DefiningTypes.AZIMUTH, sd, locationBehaviors)
      ? getLocationBehavior(DefiningTypes.AZIMUTH, sd, locationBehaviors).defining
      : false;
  const isDefiningSlowness =
    sdDefiningFromTable.slownessDefining === DefiningChange.CHANGED_TO_FALSE
      ? false
      : sdDefiningFromTable.slownessDefining === DefiningChange.CHANGED_TO_TRUE
      ? true
      : getLocationBehavior(DefiningTypes.SLOWNESS, sd, locationBehaviors)
      ? getLocationBehavior(DefiningTypes.SLOWNESS, sd, locationBehaviors).defining
      : false;
  return {
    signalDetectionId: sd.id,
    signalDetectionHypothesisId: sd.currentHypothesis.id,
    stationName: sd.stationName,
    channelName: getChannelName(sd),
    phase: SignalDetectionUtils.findPhaseFeatureMeasurementValue(
      sd.currentHypothesis.featureMeasurements
    ).phase,
    time: {
      defining: isDefiningTime,
      observed: arrivalTimeValues.obs,
      residual: arrivalTimeValues.res,
      correction: undefined
    },
    slowness: {
      defining: isDefiningSlowness,
      observed: slownessValues.obs,
      residual: slownessValues.res,
      correction: undefined
    },
    azimuth: {
      defining: isDefiningAzimuth,
      observed: azimuthValues.obs,
      residual: azimuthValues.res,
      correction: undefined
    },
    diffs: {
      isAssociatedDiff: undefined,
      slownessDefining: sdDefiningFromTable.slownessDefining,
      arrivalTimeDefining: sdDefiningFromTable.arrivalTimeDefining,
      azimuthDefining: sdDefiningFromTable.azimuthDefining
    },
    rejectedOrUnassociated: undefined
  };
}
/**
 * Creates new snapshots with false for all diffs
 * @snaps Snapshots to add false diffs to
 */
export function generateFalseDiffs(
  snaps: EventTypes.SignalDetectionSnapshot[]
): SignalDetectionSnapshotWithDiffs[] {
  return snaps.map(snap => ({
    ...snap,
    diffs: {
      isAssociatedDiff: false,
      arrivalTimeDefining: DefiningChange.NO_CHANGE,
      azimuthDefining: DefiningChange.NO_CHANGE,
      slownessDefining: DefiningChange.NO_CHANGE
    },
    rejectedOrUnassociated: false
  }));
}

export function getSnapshotsWithDiffs(
  associatedSdSnapshots: SignalDetectionSnapshotWithDiffs[],
  locationSnapshots: EventTypes.SignalDetectionSnapshot[]
): SignalDetectionSnapshotWithDiffs[] {
  // loop through the associated
  // ones that arent in location are added to our new master with "associatedDiff" true
  // ones in locationSnaps but no associated are added to master with "associatedDiff" true and rejected/unnassoc true
  const masterSnapshotList: SignalDetectionSnapshotWithDiffs[] = [];
  associatedSdSnapshots.forEach(assocSnap => {
    const maybeSameSD = locationSnapshots.find(
      ls => ls.signalDetectionId === assocSnap.signalDetectionId
    );
    if (maybeSameSD) {
      masterSnapshotList.push({
        ...assocSnap,
        diffs: {
          ...assocSnap.diffs,
          isAssociatedDiff: false,
          arrivalTimeDiff: assocSnap.time.observed !== maybeSameSD.time.observed,
          phaseDiff: assocSnap.phase !== maybeSameSD.phase,
          slownessObsDiff: assocSnap.slowness.observed !== maybeSameSD.slowness.observed,
          azimuthObsDiff: assocSnap.azimuth.observed !== maybeSameSD.azimuth.observed
        },
        rejectedOrUnassociated: false
      });
    } else {
      masterSnapshotList.push({
        ...assocSnap,
        diffs: {
          isAssociatedDiff: true,
          azimuthDefining: DefiningChange.NO_CHANGE,
          slownessDefining: DefiningChange.NO_CHANGE,
          arrivalTimeDefining: DefiningChange.NO_CHANGE
        },
        rejectedOrUnassociated: false
      });
    }
  });
  locationSnapshots.forEach(locationSnap => {
    const maybeSameSD = associatedSdSnapshots.find(
      as => as.signalDetectionId === locationSnap.signalDetectionId
    );
    if (!maybeSameSD) {
      masterSnapshotList.push({
        ...locationSnap,
        diffs: {
          isAssociatedDiff: true,
          azimuthDefining: DefiningChange.NO_CHANGE,
          slownessDefining: DefiningChange.NO_CHANGE,
          arrivalTimeDefining: DefiningChange.NO_CHANGE
        },
        rejectedOrUnassociated: true
      });
    }
  });
  return masterSnapshotList;
}

/**
 * Adds the SignalDetectionTableRowChanges to the State initially as well as when the props change.
 * @param props Current props to build the entries from.
 * @parm sdStates Current list of SignalDetectionTableRowChanges in the State
 *
 * @returns New list of SignalDetectionTableRowChanges to set in the State
 */
export function initializeSDDiffs(
  signalDetections: SignalDetectionTypes.SignalDetection[]
): SignalDetectionTableRowChanges[] {
  return signalDetections.map(sd => ({
    signalDetectionId: sd.id,
    arrivalTimeDefining: DefiningChange.NO_CHANGE,
    slownessDefining: DefiningChange.NO_CHANGE,
    azimuthDefining: DefiningChange.NO_CHANGE
  }));
}
/**
 * Helper function to remove the __typename from object.
 * Gateway GraphQL doesn't like it.
 */
export function removeTypeName(object: any): any {
  const newObj = {};
  // tslint:disable-next-line: no-for-in
  for (const key in object) {
    if (key !== '__typename') {
      newObj[key] = object[key];
    }
  }
  return newObj;
}

/**
 * Strips unwanted stuff from the channel name
 *
 * @param name the name to filter
 */
export function stripChannelName(name: string) {
  const regex = new RegExp('/(.*?) ');
  const results = regex.exec(name);
  if (results && results[0]) {
    const nameWithSlash = results[0];
    const nameWithoutSlash =
      nameWithSlash[0] === '/' ? nameWithSlash.substring(1, nameWithSlash.length) : nameWithSlash;
    return nameWithoutSlash.toUpperCase();
  }
  return '';
}

/**
 * @param sdId id of signal detection to look up
 */
export function getDefiningStatusForSdId(
  sdId: string,
  sdDefiningChanges: SignalDetectionTableRowChanges[]
): DefiningStatus {
  const maybeRow = sdDefiningChanges.find(sdr => sdr.signalDetectionId === sdId);
  if (maybeRow) {
    return {
      arrivalTimeDefining: maybeRow.arrivalTimeDefining,
      slownessDefining: maybeRow.slownessDefining,
      azimuthDefining: maybeRow.azimuthDefining
    };
  }
  return {
    arrivalTimeDefining: DefiningChange.NO_CHANGE,
    slownessDefining: DefiningChange.NO_CHANGE,
    azimuthDefining: DefiningChange.NO_CHANGE
  };
}

/**
 * Gets the signal detection snapshots, including diffs, for the latest locatino
 *
 * @param openEvent The currently open event
 * @param selectedLocationSolutionId The id for the selected location solution
 * @param sdDefiningChanges The defining changes from the location sd table
 * @param associatedSignalDetections The signal detections associated to the event
 */
export function generateSnapshotsForLatestLocation(
  openEvent: EventTypes.Event,
  selectedLocationSolutionId: string,
  sdDefiningChanges: SignalDetectionTableRowChanges[],
  associatedSignalDetections: SignalDetectionTypes.SignalDetection[]
): SignalDetectionSnapshotWithDiffs[] {
  const latestLSS = getLatestLocationSolutionSet(openEvent);
  const locationSolution =
    selectedLocationSolutionId && latestLSS
      ? latestLSS.locationSolutions.find(ls => ls.id === selectedLocationSolutionId)
        ? latestLSS.locationSolutions.find(ls => ls.id === selectedLocationSolutionId)
        : openEvent.currentEventHypothesis.eventHypothesis.preferredLocationSolution
            .locationSolution
      : undefined;

  const lastCalculatedLSS = getLatestLocationSolutionSet(openEvent);
  if (!lastCalculatedLSS || !locationSolution) {
    return [];
  }

  const locationBehaviors = locationSolution.locationBehaviors;
  const signalDetectionSnapshots = locationSolution.snapshots;
  const assocSDSnapshots = associatedSignalDetections.map(sd => {
    if (sd) {
      return convertSignalDetectionToSnapshotWithDiffs(
        sd,
        locationBehaviors,
        getDefiningStatusForSdId(sd.id, sdDefiningChanges)
      );
    }
  });
  const mergedSnapshots = getSnapshotsWithDiffs(assocSDSnapshots, signalDetectionSnapshots);
  return mergedSnapshots;
}
/**
 * Generates snapshots for a previously calculated location
 *
 * @param openEvent The currently open event
 * @param selectedLocationSolutionSetId The id of the location solution set to pull snapshots from
 * @param selectedLocationSolutionId The id of the location solution to pull snapshots from
 */
export function generateSnapshotsForPreviousLocation(
  openEvent: EventTypes.Event,
  selectedLocationSolutionSetId: string,
  selectedLocationSolutionId: string
): SignalDetectionSnapshotWithDiffs[] {
  const maybeSelectedLSS = openEvent.currentEventHypothesis.eventHypothesis.locationSolutionSets.find(
    lss => lss.id === selectedLocationSolutionSetId
  );
  if (!maybeSelectedLSS) {
    return [];
  }

  const location = maybeSelectedLSS.locationSolutions.find(
    ls => ls.id === selectedLocationSolutionId
  );
  if (!location) {
    return [];
  }
  return generateFalseDiffs(location.snapshots);
}
/**
 * Helper function to get the correct snapshots for the rendering state
 */
export function getSnapshots(
  historicalMode: boolean,
  openEvent: EventTypes.Event,
  selectedLocationSolutionSetId: string,
  selectedLocationSolutionId: string,
  sdDefiningChanges: SignalDetectionTableRowChanges[],
  associatedSignalDetections: SignalDetectionTypes.SignalDetection[]
): SignalDetectionSnapshotWithDiffs[] {
  if (historicalMode) {
    return generateSnapshotsForPreviousLocation(
      openEvent,
      selectedLocationSolutionSetId,
      selectedLocationSolutionId
    );
  }
  return generateSnapshotsForLatestLocation(
    openEvent,
    selectedLocationSolutionId,
    sdDefiningChanges,
    associatedSignalDetections
  );
}
/**
 * Returns a new location behavior with an updated defining status
 * @param behavior The Location Behavior to update
 * @param definingTableChange The change reported from the location sd table
 * @param behaviorDefiningKey The name of key in the location behavior object to udpate
 */
export function updateLocBehaviorFromTableChanges(
  behavior: EventTypes.LocationBehavior,
  definingTableChange: SignalDetectionTableRowChanges,
  behaviorDefiningKey: string
): EventTypes.LocationBehavior {
  if (definingTableChange[behaviorDefiningKey] === undefined) {
    throw new Error('value for behaviorDefiningKey not found in definingTableChange');
  }
  const change = definingTableChange[behaviorDefiningKey];
  const newBehavior = cloneDeep(behavior);
  newBehavior.defining =
    change === DefiningChange.CHANGED_TO_FALSE
      ? false
      : change === DefiningChange.CHANGED_TO_TRUE
      ? true
      : newBehavior.defining;
  return newBehavior;
}

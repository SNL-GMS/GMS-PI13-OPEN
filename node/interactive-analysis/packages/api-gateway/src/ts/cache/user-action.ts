import { toHoursMinuteSeconds } from '@gms/common-util';
import includes from 'lodash/includes';
import { PhaseType } from '../common/model';
import { EventHypothesis } from '../event/model-and-schema/model';
import {
  FeatureMeasurementTypeName,
  SignalDetection,
  SignalDetectionHypothesis
} from '../signal-detection/model';
import {
  findAmplitudeFeatureMeasurementValue,
  findArrivalTimeFeatureMeasurementValue,
  findPhaseFeatureMeasurementValue
} from '../util/feature-measurement-utils';
import { UserAction, UserActionDescription } from './model';
// tslint:disable: max-classes-per-file

/**
 * A Generic User Action.
 */
class GenericUserAction implements UserAction {
  /**
   * The user action description
   */
  public readonly description: UserActionDescription;

  /**
   * Creates an instance of UserAction.
   * @param description the unique description
   */
  public constructor(description: UserActionDescription) {
    this.description = description;
  }

  /**
   * Returns the toString value of a user action.
   * @returns the string representation
   */
  public toString(): string {
    return this.description;
  }
}

/**
 * User Action with time value displayed
 */
class EventWithTimeUserAction implements UserAction {
  /**
   * The user action description
   */
  public readonly description: UserActionDescription;

  /**
   * The event time.
   */
  public readonly time: number;

  /**
   * Creates an instance of EventWithTimeUserAction.
   *
   * @param description the unique description
   * @param time the arrival time of the detection
   * @param isCreate if we are creating
   */
  public constructor(description: UserActionDescription, time: number) {
    this.description = description;
    this.time = time;
  }

  /**
   * Returns the toString value of a user action.
   * @returns the string representation
   */
  public toString(): string {
    return `${this.description} at ${toHoursMinuteSeconds(this.time)}`;
  }
}

/**
 * Single phase on a station user action.
 */
class SinglePhaseOnStationUserAction implements UserAction {
  /**
   * The user action description
   */
  public readonly description: UserActionDescription;

  /**
   * The unique station name that the detection was on.
   */
  public readonly station: string;

  /**
   * The phase type of the detection.
   */
  public readonly phase: PhaseType;

  /**
   * The arrival time of the detection.
   */
  public readonly time: number;

  /**
   * Show time
   */
  public readonly showTime: boolean;

  /**
   * Creates an instance of SinglePhaseOnStationUserAction.
   *
   * @param description the unique description
   * @param station the station
   * @param phase the phase type of the detection
   * @param showTime used for displaying the time
   * @param time the arrival time of the detection
   */
  public constructor(
    description: UserActionDescription,
    station: string,
    phase: PhaseType,
    showTime: boolean = true,
    time: number
  ) {
    this.description = description;
    this.station = station;
    this.phase = phase;
    this.time = time;
    this.showTime = showTime;
  }

  /**
   * Returns the toString value of a user action.
   * @returns the string representation
   */
  public toString(): string {
    return this.showTime
      ? `${this.description} ${this.phase} on ${this.station} at ${toHoursMinuteSeconds(this.time)}`
      : `${this.description} ${this.phase} on ${this.station}`;
  }
}

/**
 * Update amplitude user action.
 */
class AmplitudeUserAction implements UserAction {
  /**
   * The user action description
   */
  public readonly description: UserActionDescription;

  /**
   * The unique station name that the detection was created on.
   */
  public readonly station: string;

  /**
   * The phase type of the created detection.
   */
  public readonly phase: PhaseType;

  /**
   * The old (original) value of the signal detection
   */
  public readonly oldValue: number;

  /**
   * The new (updated) value of the signal detection.
   */
  public readonly newValue: number;

  /**
   * Creates an instance of AmplitudeUserAction.
   *
   * @param description the unique description
   * @param station the station
   * @param phase the phase type of the detection
   * @param oldValue the old (original) value
   * @param newValue the new (updated) value
   */
  public constructor(
    description: UserActionDescription,
    station: string,
    phase: PhaseType,
    oldAmplitude: number,
    newAmplitude: number
  ) {
    this.description = description;
    this.station = station;
    this.phase = phase;
    this.oldValue = oldAmplitude;
    this.newValue = newAmplitude;
  }

  /**
   * Returns the toString value of a user action.
   * @returns the string representation
   */
  public toString(): string {
    // tslint:disable-next-line: max-line-length
    return `Amplitude for ${this.phase} on ${this.station} from ${this.oldValue.toFixed(
      3
    )} to ${this.newValue.toFixed(3)}`;
  }
}

/**
 * A update value user action.
 * @template T the type of the value updated
 */
class UpdateValueUserAction<T> implements UserAction {
  /**
   * The user action description
   */
  public readonly description: UserActionDescription;

  /**
   * The old (original) value of the signal detection
   */
  public readonly oldValue: T;

  /**
   * The new (updated) value of the signal detection.
   */
  public readonly newValue: T;

  /**
   * Creates an instance of UpdateDetectionUserAction.
   * @param description the unique description
   * @param oldValue the old (original) value
   * @param newValue the new (updated) value
   */
  public constructor(description: UserActionDescription, oldValue: T, newValue: T) {
    this.description = description;
    this.oldValue = oldValue;
    this.newValue = newValue;
  }

  /**
   * Returns the toString value of a user action.
   * @returns the string representation
   */
  public toString(): string {
    return `${this.description} from ${String(this.oldValue)} to ${String(this.newValue)}`;
  }
}

/**
 * A update value with station information user action.
 * @template T the type of the value updated
 */
class UpdateValueWithStationUserAction<T> extends UpdateValueUserAction<T> {
  /**
   * The unique station name that the detection was created on.
   */
  public readonly station: string;

  /**
   * Optional additional phase for retime
   */
  public readonly phaseForReTime: string;

  /**
   * Creates an instance of UpdateDetectionUserAction.
   *
   * @param description the unique description
   * @param station the station
   * @param oldValue the old (original) value
   * @param newValue the new (updated) value
   */
  public constructor(
    description: UserActionDescription,
    station: string,
    oldValue: T,
    newValue: T,
    phase?: string
  ) {
    super(description, oldValue, newValue);
    this.station = station;
    this.phaseForReTime = phase;
  }

  /**
   * Returns the toString value of a user action.
   *
   * @returns the string representation
   */
  public toString(): string {
    return this.phaseForReTime
      ? `${this.description} ${this.phaseForReTime} from ${String(this.oldValue)} to ${String(
          this.newValue
        )} on ${this.station}`
      : `${this.description} from ${String(this.oldValue)} to ${String(this.newValue)} on ${
          this.station
        }`;
  }
}

/**
 * Creates a user action based on the description provided.
 *
 * @param description the unique user action description
 * @returns the user action
 */
function createUserAction(description: UserActionDescription): UserAction {
  if (
    includes(
      [
        UserActionDescription.CREATE_DETECTION,
        UserActionDescription.UPDATE_DETECTION_RE_PHASE,
        UserActionDescription.UPDATE_DETECTION_RE_TIME,
        UserActionDescription.UPDATE_DETECTION_AMPLITUDE
      ],
      description
    )
  ) {
    throw Error(`Must use correct factory method for user action ${description}`);
  }
  return new GenericUserAction(description);
}

/**
 * Create a user action for creating a signal detection.
 *
 * @param station the station
 * @param phase the phase type of the detection
 * @param time the arrival time of the detection
 * @returns the user action
 */
function createDetectionUserAction(station: string, phase: PhaseType, time: number): UserAction {
  return new SinglePhaseOnStationUserAction(
    UserActionDescription.CREATE_DETECTION,
    station,
    phase,
    true,
    time
  );
}

/**
 * Create a user action for rejecting a signal detection.
 *
 * @param station the station
 * @param phase the phase type of the detection
 * @returns the user action
 */
function rejectDetectionUserAction(station: string, phase: PhaseType): UserAction {
  return new SinglePhaseOnStationUserAction(
    UserActionDescription.REJECT_DETECTION,
    station,
    phase,
    false,
    undefined
  );
}

/**
 * Create a user action for associate
 *
 * @param station the station
 * @param phase the phase type of the detection
 * @returns the user action
 */
function associateUserAction(station: string, phase: PhaseType): UserAction {
  return new SinglePhaseOnStationUserAction(
    UserActionDescription.CHANGE_SIGNAL_DETECTION_ASSOCIATIONS_ASSOCIATE,
    station,
    phase,
    false,
    undefined
  );
}

/**
 * Create a user action for unassociate
 *
 * @param station the station
 * @param phase the phase type of the detection
 * @returns the user action
 */
function unassociateUserAction(station: string, phase: PhaseType): UserAction {
  return new SinglePhaseOnStationUserAction(
    UserActionDescription.CHANGE_SIGNAL_DETECTION_ASSOCIATIONS_UNASSOCIATE,
    station,
    phase,
    false,
    undefined
  );
}

/**
 * Create a user action for compute fk
 *
 * @param station the station
 * @param phase the phase type of the detection
 * @returns the user action
 */
function computeFkUserAction(station: string, phase: PhaseType): UserAction {
  return new SinglePhaseOnStationUserAction(
    UserActionDescription.COMPUTE_FK,
    station,
    phase,
    false,
    undefined
  );
}

/**
 * Create event user action.
 *
 * @param time the arrival time of the detection
 * @returns the user action
 */
function createEventUserAction(time: number): UserAction {
  return new EventWithTimeUserAction(UserActionDescription.CREATE_EVENT, time);
}

/**
 * Locate event user action
 *
 * @param time the arrival time of the detection
 * @returns the user action
 */
function locateEventUserAction(time: number): UserAction {
  return new EventWithTimeUserAction(UserActionDescription.UPDATE_EVENT_LOCATE, time);
}

/**
 * Create a user action for re-phasing a signal detection.
 *
 * @param station the station
 * @param oldPhase the old (original) phase of the signal detection
 * @param newPhase the new (updated) phase of the signal detection
 * @returns the user action
 */
function createUpdateDetectionRePhaseUserAction(
  station: string,
  oldPhase: PhaseType,
  newPhase: PhaseType
): UserAction {
  return new UpdateValueWithStationUserAction<PhaseType>(
    UserActionDescription.UPDATE_DETECTION_RE_PHASE,
    station,
    oldPhase,
    newPhase
  );
}

/**
 * Create a user action for re-timing a signal detection.
 *
 * @param station the station
 * @param oldTime the old (original) time of the signal detection
 * @param newTime the new (updated) time of the signal detection
 * @returns the user action
 */
function createUpdateDetectionReTimeUserAction(
  station: string,
  oldTime: number,
  newTime: number,
  phase: string
): UserAction {
  return new UpdateValueWithStationUserAction<string>(
    UserActionDescription.UPDATE_DETECTION_RE_TIME,
    station,
    toHoursMinuteSeconds(oldTime),
    toHoursMinuteSeconds(newTime),
    phase
  );
}

/**
 * Create a user action for updating the amplitude measurement of a signal detection.
 *
 * @param station the station
 * @param oldValue the old (original) amplitude measurement of the signal detection
 * @param newValue the new (updated) amplitude measurement of the signal detection
 * @returns the user action
 */
function createUpdateDetectionAmplitudeUserAction(
  station: string,
  phase: PhaseType,
  oldAmplitude: number,
  newAmplitude: number
): UserAction {
  return new AmplitudeUserAction(
    UserActionDescription.REJECT_DETECTION,
    station,
    phase,
    oldAmplitude,
    newAmplitude
  );
}

/**
 * Create a user action for a event hypothesis change.
 *
 * @param description The description of the user action
 * @param oldEventHypothesis the old (original) event hypothesis
 * @param newEventHypothesis the new (updated) event hypothesis
 */
export function userActionCreatorForEventHypothesisChange(
  description: UserActionDescription,
  oldEventHypothesis: EventHypothesis,
  newEventHypothesis: EventHypothesis
): UserAction {
  switch (description) {
    case UserActionDescription.CREATE_EVENT:
      return createEventUserAction(
        newEventHypothesis.preferredLocationSolution.locationSolution.location.time
      );
    case UserActionDescription.UPDATE_EVENT_LOCATE:
      return locateEventUserAction(
        newEventHypothesis.preferredLocationSolution.locationSolution.location.time
      );
    case UserActionDescription.CREATE_DETECTION:
    case UserActionDescription.REJECT_DETECTION:
    case UserActionDescription.REJECT_MULTIPLE_DETECTIONS:
    case UserActionDescription.UPDATE_DETECTION_RE_TIME:
    case UserActionDescription.UPDATE_DETECTION_RE_PHASE:
    case UserActionDescription.UPDATE_MULTIPLE_DETECTIONS_RE_PHASE:
    case UserActionDescription.UPDATE_DETECTION_AMPLITUDE:
    case UserActionDescription.UPDATE_DETECTION_REVIEW_AMPLITUDE:
    case UserActionDescription.UPDATE_DETECTION:
    case UserActionDescription.CHANGE_SIGNAL_DETECTION_ASSOCIATIONS_ASSOCIATE_MULTIPLE:
    case UserActionDescription.CHANGE_SIGNAL_DETECTION_ASSOCIATIONS_UNASSOCIATE_MULTIPLE:
    case UserActionDescription.CHANGE_SIGNAL_DETECTION_ASSOCIATIONS_ASSOCIATE:
    case UserActionDescription.CHANGE_SIGNAL_DETECTION_ASSOCIATIONS_UNASSOCIATE:
    case UserActionDescription.COMPUTE_FK:
    case UserActionDescription.COMPUTE_MULTIPLE_FK:
      return createUserAction(UserActionDescription.UPDATE_EVENT_FROM_SIGNAL_DETECTION_CHANGE);
    default:
      return createUserAction(description);
  }
}

/**
 * Create a user action for a signal detection hypothesis change.
 *
 * @param description The description of the user action
 * @param signalDetection the signal detection that the hypothesis are associated too
 * @param oldSignalDetectionHypothesis the old (original) signal detection hypothesis
 * @param newSignalDetectionHypothesis the new (updated) signal detection hypothesis
 */
export function userActionCreatorForSignalDetectionHypothesisChange(
  description: UserActionDescription,
  signalDetection: SignalDetection,
  oldSignalDetectionHypothesis: SignalDetectionHypothesis,
  newSignalDetectionHypothesis: SignalDetectionHypothesis
): UserAction {
  switch (description) {
    case UserActionDescription.CREATE_DETECTION:
      return createDetectionUserAction(
        signalDetection.stationName,
        findPhaseFeatureMeasurementValue(newSignalDetectionHypothesis.featureMeasurements).phase,
        findArrivalTimeFeatureMeasurementValue(newSignalDetectionHypothesis.featureMeasurements)
          .value
      );
    case UserActionDescription.REJECT_DETECTION:
    case UserActionDescription.REJECT_MULTIPLE_DETECTIONS:
      return rejectDetectionUserAction(
        signalDetection.stationName,
        findPhaseFeatureMeasurementValue(newSignalDetectionHypothesis.featureMeasurements).phase
      );
    case UserActionDescription.UPDATE_DETECTION_RE_PHASE:
    case UserActionDescription.UPDATE_MULTIPLE_DETECTIONS_RE_PHASE:
      return createUpdateDetectionRePhaseUserAction(
        signalDetection.stationName,
        findPhaseFeatureMeasurementValue(oldSignalDetectionHypothesis.featureMeasurements).phase,
        findPhaseFeatureMeasurementValue(newSignalDetectionHypothesis.featureMeasurements).phase
      );
    case UserActionDescription.UPDATE_DETECTION_RE_TIME:
      return createUpdateDetectionReTimeUserAction(
        signalDetection.stationName,
        findArrivalTimeFeatureMeasurementValue(oldSignalDetectionHypothesis.featureMeasurements)
          .value,
        findArrivalTimeFeatureMeasurementValue(newSignalDetectionHypothesis.featureMeasurements)
          .value,
        findPhaseFeatureMeasurementValue(newSignalDetectionHypothesis.featureMeasurements).phase
      );
    case UserActionDescription.UPDATE_DETECTION_AMPLITUDE:
      return createUpdateDetectionAmplitudeUserAction(
        signalDetection.stationName,
        findPhaseFeatureMeasurementValue(newSignalDetectionHypothesis.featureMeasurements).phase,
        findAmplitudeFeatureMeasurementValue(
          oldSignalDetectionHypothesis.featureMeasurements,
          FeatureMeasurementTypeName.AMPLITUDE_A5_OVER_2
        ).amplitude.value,
        findAmplitudeFeatureMeasurementValue(
          newSignalDetectionHypothesis.featureMeasurements,
          FeatureMeasurementTypeName.AMPLITUDE_A5_OVER_2
        ).amplitude.value
      );
    case UserActionDescription.CREATE_EVENT:
    case UserActionDescription.CHANGE_SIGNAL_DETECTION_ASSOCIATIONS_ASSOCIATE:
    case UserActionDescription.CHANGE_SIGNAL_DETECTION_ASSOCIATIONS_ASSOCIATE_MULTIPLE:
      return associateUserAction(
        signalDetection.stationName,
        findPhaseFeatureMeasurementValue(newSignalDetectionHypothesis.featureMeasurements).phase
      );
    case UserActionDescription.CHANGE_SIGNAL_DETECTION_ASSOCIATIONS_UNASSOCIATE:
    case UserActionDescription.CHANGE_SIGNAL_DETECTION_ASSOCIATIONS_UNASSOCIATE_MULTIPLE:
      return unassociateUserAction(
        signalDetection.stationName,
        findPhaseFeatureMeasurementValue(newSignalDetectionHypothesis.featureMeasurements).phase
      );
    case UserActionDescription.COMPUTE_FK:
    case UserActionDescription.COMPUTE_MULTIPLE_FK:
      return computeFkUserAction(
        signalDetection.stationName,
        findPhaseFeatureMeasurementValue(newSignalDetectionHypothesis.featureMeasurements).phase
      );
    default:
      return createUserAction(description);
  }
}

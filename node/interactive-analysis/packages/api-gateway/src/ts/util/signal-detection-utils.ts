import { MILLISECONDS_IN_SECOND, uuid4 } from '@gms/common-util';
import produce from 'immer';
import flatMap from 'lodash/flatMap';
import includes from 'lodash/includes';
import { ChannelSegment, TimeSeries } from '../channel-segment/model';
import { PhaseType, TimeRange, Units } from '../common/model';
import { ConfigProcessor } from '../config/config-processor';
import { Event } from '../event/model-and-schema/model';
import { getEventHyposAssocToSd } from '../event/utils/event-utils';
import {
  AmplitudeMeasurementValue,
  FeatureMeasurement,
  FeatureMeasurementTypeName,
  FeatureMeasurementValue,
  InstantMeasurementValue,
  MeasuredChannelSegmentDescriptor,
  SignalDetection,
  SignalDetectionHypothesis,
  SignalDetectionTimingInput
} from '../signal-detection/model';
import { ProcessingChannel } from '../station/processing-station/model';
import { createFeatureMeasurementId } from './feature-measurement-convert-utils';
import {
  findAmplitudeFeatureMeasurement,
  findArrivalTimeFeatureMeasurement,
  findArrivalTimeFeatureMeasurementValue,
  findPhaseFeatureMeasurement
} from './feature-measurement-utils';

/**
 * Returns true if sd is on one of the given stations or in the given time range
 * @param signalDetection signal detection to check
 * @param stationIds stations ids to check for
 * @param timeRange time range to check in
 *
 * @returns true if the given sd is in the time range or part of the stations given
 */
export function filterSdsByStationAndTime(
  signalDetection: SignalDetection,
  stationIds: string[],
  timeRange: TimeRange
): boolean {
  if (!signalDetection) {
    return false;
  }
  // Retrieve the arrival time feature measurement from the current hypothesis
  const hypFMs = signalDetection.currentHypothesis.featureMeasurements;
  const arrivalTimeMeasurementValue = findArrivalTimeFeatureMeasurementValue(hypFMs);
  const arrivalTimeEpoch = arrivalTimeMeasurementValue.value;
  const matchedStation = stationIds.indexOf(signalDetection.stationName) > -1;
  const startTimeMatch = arrivalTimeEpoch >= timeRange.startTime;
  const endTimeMatch = arrivalTimeEpoch <= timeRange.endTime;

  // Return true if the detection's station ID matches the input list
  // and the arrival time is in the input time range
  return matchedStation && startTimeMatch && endTimeMatch;
}

/**
 * Gets the signal detections associated to an event
 * @param event event
 * @param sds all sds available
 */
export function getSignalDetectionsAssociatedToEvent(
  event: Event,
  sds: SignalDetection[]
): SignalDetection[] {
  return Object.seal(sds.filter(sd => includes(event.signalDetectionIds, sd.id)));
}

/**
 * Creates a signal detection
 * @param stationName stations id to create signal detection on
 * @param phase phase to set
 * @param arrivalTime arrival time to set
 * @param eventId event id to associate to
 * @param amplitudeMeasurement amplitude measurement
 *
 * @returns newly created signal detection
 */
export function createSignalDetection(
  stationName: string,
  phase: PhaseType,
  arrivalTime: number,
  channel: ProcessingChannel,
  amplitudeMeasurement?: AmplitudeMeasurementValue
): SignalDetection {
  const monitoringOrganization = 'CTBTO';
  const detectionId = uuid4();
  const newHypothesis = createSdHypothesis(
    detectionId,
    stationName,
    monitoringOrganization,
    phase,
    arrivalTime,
    channel,
    amplitudeMeasurement
  );
  // Create a new signal detection
  return {
    id: detectionId,
    monitoringOrganization,
    stationName,
    signalDetectionHypotheses: Object.seal([newHypothesis]),
    currentHypothesis: newHypothesis,
    associations: [],
    hasConflict: false
  };
}

/**
 * Creates a new signal detection hypothesis
 * @param detectionId detection id of the parent
 * @param eventId the event id the detection is associated with
 * @param phase phase to set
 * @param arrivalTime arrival time to set
 * @param amplitudeMeasurement amplitude measurement to set
 */
function createSdHypothesis(
  detectionId: string,
  stationName: string,
  monitoringOrganization: string,
  phase: PhaseType,
  arrivalTime: number,
  channel: ProcessingChannel,
  amplitudeMeasurement?: AmplitudeMeasurementValue
) {
  const hypothesisId = uuid4();

  // TODO: Create the FK_Beam and use the start/end time for now
  // just adding 5 minutes to arrival time
  const fiveMinutes = 300;
  const endTime = arrivalTime + fiveMinutes;
  // Create a new signal detection hypothesis
  const newHypothesis: SignalDetectionHypothesis = {
    id: hypothesisId,
    stationName,
    monitoringOrganization,
    rejected: false,
    parentSignalDetectionId: detectionId,
    parentSignalDetectionHypothesisId: undefined, // New SD means no parent
    modified: true,
    reviewed: {
      amplitudeMeasurement: false
    },
    featureMeasurements: Object.seal(
      [
        createFeatureMeasurement(
          FeatureMeasurementTypeName.ARRIVAL_TIME,
          {
            value: arrivalTime,
            standardDeviation: 0
          },
          channel,
          arrivalTime,
          endTime
        ),
        createFeatureMeasurement(
          FeatureMeasurementTypeName.RECEIVER_TO_SOURCE_AZIMUTH,
          {
            referenceTime: arrivalTime,
            measurementValue: {
              value: 21.73,
              standardDeviation: 0.31,
              units: Units.SECONDS_PER_DEGREE
            }
          },
          channel,
          arrivalTime,
          endTime
        ),
        createFeatureMeasurement(
          FeatureMeasurementTypeName.SLOWNESS,
          {
            referenceTime: arrivalTime,
            measurementValue: {
              value: 21.73,
              standardDeviation: 0.31,
              units: Units.SECONDS_PER_DEGREE
            }
          },
          channel,
          arrivalTime,
          endTime
        ),
        createFeatureMeasurement(
          FeatureMeasurementTypeName.PHASE,
          {
            phase,
            confidence: 1
          },
          channel,
          arrivalTime,
          endTime
        ),
        amplitudeMeasurement
          ? createFeatureMeasurement(
              FeatureMeasurementTypeName.AMPLITUDE_A5_OVER_2,
              {
                startTime: amplitudeMeasurement.startTime,
                period: amplitudeMeasurement.period,
                amplitude: amplitudeMeasurement.amplitude
              },
              channel,
              arrivalTime,
              endTime
            )
          : undefined
      ].filter(fm => fm !== undefined)
    )
  };
  return newHypothesis;
}

/**
 * Creates a feature measurement
 * @param featureMeasurementType Type of FM
 * @param measurementValue the value desired
 * @param channelSegmentId channel segment to set
 * @param phase phase to set
 *
 * @returns newly created feature measurement
 */
export function createFeatureMeasurement<T extends FeatureMeasurementValue>(
  featureMeasurementType: FeatureMeasurementTypeName,
  measurementValue: T,
  channel: ProcessingChannel,
  startTime: number,
  endTime: number
): FeatureMeasurement {
  const measuredChannelSegmentDescriptor: MeasuredChannelSegmentDescriptor = {
    channelName: channel.name,
    measuredChannelSegmentStartTime: startTime,
    measuredChannelSegmentEndTime: endTime,
    measuredChannelSegmentCreationTime: Date.now() / MILLISECONDS_IN_SECOND
  };
  const fm: FeatureMeasurement = {
    id: createFeatureMeasurementId(measuredChannelSegmentDescriptor, featureMeasurementType),
    channel,
    measuredChannelSegmentDescriptor,
    featureMeasurementType,
    measurementValue
  };
  return fm;
}

/**
 * Gets an arrival time for the given input. Returns a newly created
 * arrival time feature measurement or the original.
 * @param arrivalTimeFm the arrival time fm to modify
 * @param sdTiming the user input
 *
 * @returns feature measurement
 */
export function getArrivalTimeForInput(
  arrivalTimeFm: FeatureMeasurement,
  sdTiming?: SignalDetectionTimingInput
): FeatureMeasurement {
  if (sdTiming && sdTiming.arrivalTime && sdTiming.timeUncertaintySec) {
    return produce<FeatureMeasurement>(arrivalTimeFm, draftState => {
      draftState.measurementValue = {
        standardDeviation: sdTiming.timeUncertaintySec,
        value: sdTiming.arrivalTime
      };
    });
  }
  return arrivalTimeFm;
}

/**
 * Gets an A5 feature measurement for the input. Returns a newly created
 * A5 feature measurement or the original.
 * @param startTime the start time
 * @param endTime the end time
 * @param aFiveAmplitudeFm the A five amplitude feature measurement
 * @param sdTiming the signal detection time
 * @param channel the channel
 */
export function getAFiveAmplitudeForInput(
  startTime: number,
  endTime: number,
  aFiveAmplitudeFm: FeatureMeasurement,
  sdTiming: SignalDetectionTimingInput,
  channel: ProcessingChannel
): FeatureMeasurement {
  if (aFiveAmplitudeFm) {
    return produce<FeatureMeasurement>(aFiveAmplitudeFm, draftState => {
      draftState.measurementValue = {
        startTime: sdTiming.amplitudeMeasurement.startTime,
        period: sdTiming.amplitudeMeasurement.period,
        amplitude: sdTiming.amplitudeMeasurement.amplitude
      };
    });
  }
  return createFeatureMeasurement(
    FeatureMeasurementTypeName.AMPLITUDE_A5_OVER_2,
    sdTiming.amplitudeMeasurement,
    channel,
    startTime,
    endTime
  );
}

/**
 * Gets a phase feature measurement for the input. Returns a newly created
 * phase feature measurement or the original.
 * @param phaseFm the measurement to update
 * @param maybePhase the phase input - maybe undefined
 */
export function getPhaseFmForInput(
  phaseFm: FeatureMeasurement,
  phase?: PhaseType
): FeatureMeasurement {
  if (phase) {
    return produce<FeatureMeasurement>(phaseFm, draftState => {
      draftState.measurementValue = {
        phase,
        confidence: 1
      };
    });
  }
  return phaseFm;
}

/**
 * Creates fms in response to user input
 * @param featureMeasurements A list of feature measurements to modify. Use clone deep if you wish to preserve old fms
 * @param channel processing channel for the fms to be added to
 * @param phase the phase input
 * @param sdTiming the timing information input
 *
 * @returns feature measurement array
 */
export function createFmsFromUserInput(
  featureMeasurements: FeatureMeasurement[],
  channel: ProcessingChannel,
  phase?: string,
  sdTiming?: SignalDetectionTimingInput
): FeatureMeasurement[] {
  // Update Arrival Time FM
  const oldArrivalTimeFM = findArrivalTimeFeatureMeasurement(featureMeasurements);

  const newArrivalTimeFm = getArrivalTimeForInput(oldArrivalTimeFM, sdTiming);

  // TODO: if FK_Beam were created we could get start/end time from that
  const fiveMinutes = 300;
  const startTime = (newArrivalTimeFm.measurementValue as InstantMeasurementValue).value;
  const endTime = startTime + fiveMinutes;

  // Update A Five over 2 measurement
  const oldAFiveAmplitudeFm = findAmplitudeFeatureMeasurement(
    featureMeasurements,
    FeatureMeasurementTypeName.AMPLITUDE_A5_OVER_2
  );

  const newAmplitudeFm =
    sdTiming && sdTiming.amplitudeMeasurement
      ? getAFiveAmplitudeForInput(startTime, endTime, oldAFiveAmplitudeFm, sdTiming, channel)
      : oldAFiveAmplitudeFm;

  // Update phase feature measurement
  const oldPhaseFm = findPhaseFeatureMeasurement(featureMeasurements);

  const newPhaseFm = getPhaseFmForInput(oldPhaseFm, phase as PhaseType);

  // Update Feature measurements
  const updatedFmTypes = [
    newArrivalTimeFm ? newArrivalTimeFm.featureMeasurementType : undefined,
    newAmplitudeFm ? newAmplitudeFm.featureMeasurementType : undefined,
    newPhaseFm ? newPhaseFm.featureMeasurementType : undefined
  ].filter(fmType => fmType !== undefined);

  return [
    newAmplitudeFm,
    newArrivalTimeFm,
    newPhaseFm,
    ...featureMeasurements.filter(
      fm => !updatedFmTypes.find(ufm => ufm === fm.featureMeasurementType)
    )
  ].filter(fm => fm !== undefined);
}

/**
 * Determines if an event is associated to unsaved events
 * @param sd signal detection to check
 * @param events events to get associations from
 *
 * @returns true if sd has modified associations
 */
export function sdHasModifiedAssociations(sd: SignalDetection, events: Event[]): boolean {
  const hypId = sd.currentHypothesis.id;
  const assocHyps = getEventHyposAssocToSd(events, [hypId]);
  const associations = flatMap(assocHyps, hyp =>
    hyp.associations.filter(assoc => assoc.signalDetectionHypothesisId === hypId)
  );

  return associations
    .map(assoc => assoc.modified)
    .reduce((accumulator, value) => accumulator || value, false);
}

/**
 * Determines if phase needs amplitude review - TBD configurable
 * @param phase the phase
 */
export function doesPhaseNeedAmplitudeReview(phase: PhaseType): boolean {
  if (includes(ConfigProcessor.Instance().getConfigByKey('amplitudePhases'), phase)) {
    return true;
  }
  return false;
}

/**
 * Returns sd hyp associated to the event if it exists
 * @param sd sd to search for hypothesis
 * @param eventId id to find associated hypothesis for
 */
export function getSignalDetectionHypForEvent(
  sd: SignalDetection,
  eventId: string
): SignalDetectionHypothesis {
  return sd.signalDetectionHypotheses.find(
    hyp =>
      sd.associations.find(
        association =>
          association.eventId === eventId && association.signalDetectionHypothesisId === hyp.id
      ) !== undefined
  );
}

/**
 * s
 * @param allDetections the detections
 * @param hypId the hypothesis id
 */
export function getSignalDetectionByHypId(
  allDetections: SignalDetection[],
  hypId: string
): SignalDetection {
  return allDetections.find(
    sd => sd.signalDetectionHypotheses.find(sdh => sdh.id === hypId) !== undefined
  );
}

/**
 * Signal Detection Helper function to create Feature Measurement's
 * Channel Segment Descriptor from a new Channel Segment
 *
 * @param Channel Segment
 * @return MeasuredChannelSegmentDescriptor
 */
export function getChannelSegmentDescriptor(
  cs: ChannelSegment<TimeSeries>
): MeasuredChannelSegmentDescriptor {
  return {
    channelName: cs.channel.name,
    measuredChannelSegmentCreationTime: Date.now() / MILLISECONDS_IN_SECOND,
    measuredChannelSegmentStartTime: cs.startTime,
    measuredChannelSegmentEndTime: cs.endTime
  };
}

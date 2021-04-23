import { getDurationTime, setDurationTime, toEpochSeconds, toOSDTime } from '@gms/common-util';
import isEqual from 'lodash/isEqual';
import { UserContext } from '../cache/model';
import { ChannelSegment, OSDChannelSegment, TimeSeriesType } from '../channel-segment/model';
import { PhaseType } from '../common/model';
import {
  FkConfiguration,
  FkPowerSpectra,
  FkPowerSpectraOSD,
  FkPowerSpectrum,
  FkPowerSpectrumOSD,
  FstatData
} from '../fk/model';
import { SignalDetectionHypothesis } from '../signal-detection/model';
import { ProcessingStationProcessor } from '../station/processing-station/processing-station-processor';
import { OSDWaveform, Waveform } from '../waveform/model';
import { fixNaNValuesDoubleArray } from './common-utils';
import { findPhaseFeatureMeasurementValue } from './feature-measurement-utils';

/**
 * Converts the OSD Waveform into a API Gateway compatible Waveform
 * @param osdWaveform the osd waveform
 * @returns Waveform converted OSD Waveform to API Gateway compatible
 */
export function convertOSDWaveformToWaveform(osdWaveform: OSDWaveform): Waveform {
  return {
    ...osdWaveform,
    startTime: toEpochSeconds(osdWaveform.startTime)
  };
}

/**
 * Convert the OSD Fk Power Spectra to API Gateway compatible Fk Power Spectra
 * @param fkChannelSegment Fk Power Spectra Channel Segment
 * @param sd signal detection
 * @returns a FkPowerSpectra ChannelSegment
 */
export function convertFkFromOSDtoAPI(
  userContext: UserContext,
  fkChannelSegment: OSDChannelSegment<FkPowerSpectraOSD>,
  sdHyp: SignalDetectionHypothesis,
  stationName: string
): ChannelSegment<FkPowerSpectra> {
  const startTime: number = toEpochSeconds(fkChannelSegment.startTime);
  const seconds: number =
    fkChannelSegment.timeseries[0].sampleRate * fkChannelSegment.timeseries[0].sampleCount;
  const fk: ChannelSegment<FkPowerSpectra> = {
    ...fkChannelSegment,
    startTime,
    endTime: startTime + seconds,
    requiresSave: false, // Coming from OSD means no changes to save
    timeseries: fkChannelSegment.timeseries.map(ts => {
      // Copy the values to be converted to spectrums and remove values entry
      const values = ts.values;
      const spectrums = values.map(value => ({
        ...value,
        attributes: value.attributes[0]
      }));
      delete ts.values;
      // Create the timeseries [0] entry
      const tsEntry: FkPowerSpectra = {
        ...ts,
        lowFrequency: 1,
        highFrequency: 4,
        slowCountX: 1,
        slowCountY: 1,
        metadata: ts.metadata,
        reviewed: false, // Since new from OSD mark as not reviewed by analyst
        windowLead: ts.windowLead ? getDurationTime(ts.windowLead) : 1,
        // tslint:disable-next-line: no-magic-numbers
        windowLength: ts.windowLength ? getDurationTime(ts.windowLength) : 4,
        stepSize: 1,
        startTime: toEpochSeconds(ts.startTime),
        spectrums,
        fstatData: undefined, // Populated after initialization
        configuration: getDefaultFkConfigurationForSignalDetection(
          findPhaseFeatureMeasurementValue(sdHyp.featureMeasurements).phase,
          stationName
        )
      };
      // Check FstatGrid and PowerGrid for NaN
      tsEntry.spectrums.forEach(spectrum => {
        fixNaNValuesDoubleArray(spectrum.fstat);
        fixNaNValuesDoubleArray(spectrum.power);
      });
      setFstatData(tsEntry);
      return tsEntry;
    })
  };
  return fk;
}

/**
 * Convert the OSD Fk Power Spectra to API Gateway compatible Fk Power Spectra
 * @param fkChannelSegment Fk Power Spectra API Channel Segment
 * @param sd signal detection
 * @returns a FkPowerSpectra ChannelSegment
 */
export function convertFktoOSDfromAPI(timeseries: FkPowerSpectra[]): FkPowerSpectraOSD[] {
  const osdTimeSeries = timeseries.map(ts => {
    // Convert the values to attributes list for OSD
    const values: FkPowerSpectrumOSD[] = ts.spectrums.map(value => ({
      ...value,
      attributes: [
        {
          ...value.attributes,
          xSlow: 0.41,
          ySlow: 0.41
        }
      ]
    }));
    // Create the timeseries [0] entry
    const tsEntry: FkPowerSpectraOSD = {
      metadata: ts.metadata,
      sampleRate: ts.sampleRate,
      sampleCount: ts.sampleCount,
      lowFrequency: ts.lowFrequency,
      highFrequency: ts.highFrequency,
      type: ts.type,
      // Since new from OSD mark as not reviewed by analyst
      windowLead: setDurationTime(ts.windowLead),
      windowLength: setDurationTime(ts.windowLength),
      startTime: toOSDTime(ts.startTime),
      values
    };
    // Check FstatGrid and PowerGrid for NaN
    tsEntry.values.forEach(spectrum => {
      fixNaNValuesDoubleArray(spectrum.fstat);
      fixNaNValuesDoubleArray(spectrum.power);
    });
    return tsEntry;
  });
  return osdTimeSeries;
}

/**
 * Returns an empty FK Spectrum configuration. The values are NOT default values,
 * but instead values that will make it obvious within the UI that a correct
 * configuration was never added to the FK
 * @returns a FKConfiguration
 */
const defaultFkConfiguration: FkConfiguration = {
  contributingChannelsConfiguration: [],
  maximumSlowness: 40,
  mediumVelocity: 1,
  normalizeWaveforms: false,
  numberOfPoints: 81,
  useChannelVerticalOffset: false,
  leadFkSpectrumSeconds: 1
};

/**
 * Returns an Fk Configuration for the correct phase
 */
export function getDefaultFkConfigurationForSignalDetection(phase: PhaseType, stationId: string) {
  const phaseAsString = PhaseType[phase];
  const channels = ProcessingStationProcessor.Instance().getChannelsByStation(stationId);
  const contributingChannelsConfiguration = channels.map(channel => ({
    name: channel.name,
    id: channel.name, // TODO: Remove this as part of fk rework
    enabled: true
  }));
  let mediumVelocity = 0;
  if (phaseAsString.toLowerCase().startsWith('p') || phaseAsString.toLowerCase().endsWith('p')) {
    // tslint:disable-next-line: no-magic-numbers
    mediumVelocity = 5.8;
  } else if (
    phaseAsString.toLowerCase().startsWith('s') ||
    phaseAsString.toLowerCase().endsWith('s')
  ) {
    // tslint:disable-next-line: no-magic-numbers
    mediumVelocity = 3.6;
  } else if (phaseAsString === PhaseType.Lg) {
    // tslint:disable-next-line: no-magic-numbers
    mediumVelocity = 3.5;
  } else if (phaseAsString === PhaseType.Rg) {
    // tslint:disable-next-line: number-literal-format
    mediumVelocity = 3.0;
  } else {
    // Cause Tx or N...undefined behavior ok
    mediumVelocity = 1;
  }
  const fkConfiguration: FkConfiguration = {
    ...defaultFkConfiguration,
    mediumVelocity,
    contributingChannelsConfiguration
  };
  return fkConfiguration;
}

/**
 * Approximate conversion between km and degrees
 */
export function kmToDegreesApproximate(km: number) {
  const DEGREES_IN_CIRCLE = 360;
  const RAD_EARTH = 6371;
  const TWO_PI = Math.PI * 2;
  return km * (DEGREES_IN_CIRCLE / (RAD_EARTH * TWO_PI));
}

/**
 * Create the FstatData for use by the Fk Plots in the Az/Slow component
 * @param signalDetHypo Signal Detection Hypothesis
 * @param fkSpectra FkPowerData
 *
 * @returns FK Stat Data or undefined if not able to create
 */
export function setFstatData(fkSpectra: FkPowerSpectra) {
  const fstatData = convertToPlotData(fkSpectra);
  // Cache it in the FK Power Spectra
  fkSpectra.fstatData = fstatData;
}

/**
 * Convert a FkSpectra (received from COI or Streaming Service) into an FstatData representation.
 * @param fkSpectra: FkPowerSpectra from COI/Streaming Service
 * @param beamWaveform: beam from the SD Arrival Time Feature measurement Channel Segment
 * @param arrivalTime: arrival time value
 *
 * @returns FK Stat Data or undefined if not able to create
 */
function convertToPlotData(fkSpectra: FkPowerSpectra): FstatData | undefined {
  // If the channel segment is populated at the top properly
  if (!fkSpectra) {
    return undefined;
  }
  const fstatData: FstatData = {
    azimuthWf: createFkWaveform(fkSpectra),
    fstatWf: createFkWaveform(fkSpectra),
    slownessWf: createFkWaveform(fkSpectra)
  };

  // Populate fstatData waveforms beams was a parameter
  if (fkSpectra && fkSpectra.spectrums) {
    fkSpectra.spectrums.forEach((fkSpectrum: FkPowerSpectrum) => {
      fstatData.azimuthWf.values.push(fkSpectrum.attributes.azimuth);
      fstatData.fstatWf.values.push(fkSpectrum.attributes.peakFStat);
      fstatData.slownessWf.values.push(fkSpectrum.attributes.slowness);
    });
  }
  return fstatData;
}

/**
 * Helper method to create the FkData waveforms (azimuthWf, fstatWf, slownessWf)
 * @param fkSpectra the fk spectra
 */
function createFkWaveform(fkSpectra: FkPowerSpectra): Waveform {
  const waveform = {
    sampleRate: fkSpectra.sampleRate,
    sampleCount: fkSpectra.sampleCount,
    startTime: fkSpectra.startTime + fkSpectra.windowLead,
    type: TimeSeriesType.FK_SPECTRA,
    values: []
  };
  return waveform;
}
/**
 * Checks if the returned value from a service doesn't contain any data
 * @param maybeFkOsd is maybe fk osd
 */
export function isEmptyReturnFromFkService(maybeFkOsd: any): boolean {
  if (!maybeFkOsd) {
    return true;
  }
  if (!maybeFkOsd[0] || maybeFkOsd[0] === null) {
    return true;
  }
  return false;
}

/**
 * Calculates start time for fk service
 * @param wfStartTime start of the signal detection beam
 * @param arrivalTime arrival time of the signal detection
 * @param leadTime lead time for fk calculation
 * @param stepSize step size for fk calculation
 *
 * @return epoch seconds representing the start time for fk calculation
 */
export function calculateStartTimeForFk(
  wfStartTime: number,
  arrivalTime: number,
  leadTime: number,
  stepSize: number
): number {
  if (
    wfStartTime === undefined ||
    arrivalTime === undefined ||
    leadTime === undefined ||
    stepSize === undefined
  ) {
    // tslint:disable-next-line: no-console
    console.error('Cannot calculate fk start time with undefined parameters');
    return undefined;
  }
  const stepTime = arrivalTime - wfStartTime - leadTime;
  const numberOfSteps = Math.floor(stepTime / stepSize);
  if (numberOfSteps < 0) {
    // tslint:disable-next-line: no-console
    console.error(
      'Cannot calculate fk start time. Wf start time is not far enough before arrival time'
    );
    return undefined;
  }
  const timeBeforeArrival = stepSize * numberOfSteps + leadTime;
  return arrivalTime - timeBeforeArrival;
}

/**
 * Compare the FstatGrids in each spectrum to the next to see if there
 * are duplicated Grids being returned sequentially.
 * @param fkSpectrums the fk spectrums
 */
export function compareFkSpectrumGrids(fkSpectrums: FkPowerSpectrum[]) {
  // Walk the list comparing when different and printing the index changes
  fkSpectrums.forEach((fkSpectrum, index) => {
    if (fkSpectrums.length - 1 > index + 1) {
      if (isEqual(fkSpectrums[index].fstat, fkSpectrums[index + 1].fstat)) {
        console.warn(`Duplicate fstat grid found at indices ${index} - ${index + 1}`);
      }
    }
  });
}

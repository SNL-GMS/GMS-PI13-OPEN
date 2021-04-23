import { EventTypes, ProcessingStationTypes, SignalDetectionTypes } from '@gms/common-graphql';
import { UILogger } from '@gms/ui-apollo';
import { isSignalDetectionUnassociated } from '~analyst-ui/common/utils/signal-detection-util';
import { analystUiConfig } from '~analyst-ui/config';
import { semanticColors } from '~scss-config/color-preferences';

declare var Cesium;

/**
 * Cesium DataSource with signal detections
 *
 * @param dataSource source of signal detection data
 * @param signalDetections list of map signal detections
 * @param nextOpenEventId id of incoming open event
 */
export function draw(
  dataSource: any,
  signalDetections: SignalDetectionTypes.SignalDetection[],
  events: EventTypes.Event[],
  currentlyOpenEvent: EventTypes.Event,
  defaultStations: ProcessingStationTypes.ProcessingStation[]
) {
  addSignalDetections(dataSource, signalDetections, events, currentlyOpenEvent, defaultStations);
}

/**
 * Highlights the selected detection from either map/waveform viewer/list click
 *
 * @param dataSource signal detection datasource
 * @param nextSelectedSignalDetection detections being selected
 */
export function highlightSelectedSignalDetections(
  dataSource: any,
  nextSelectedSignalDetection: string[]
) {
  const unSelectedWidth = analystUiConfig.userPreferences.map.widths.unselectedSignalDetection;
  const selectedWidth = analystUiConfig.userPreferences.map.widths.selectedSignalDetection;

  const signalDetectionEntities = dataSource.entities.values.map(entity => entity.id);
  const l = signalDetectionEntities.length;

  for (let i = 0; i < l; i++) {
    const sdID = signalDetectionEntities[i];
    const usd = dataSource.entities.getById(sdID);
    usd.polyline.width = unSelectedWidth;
  }

  nextSelectedSignalDetection.forEach(function(nsd) {
    const nextSignalDetection = dataSource.entities.getById(nsd);
    if (nextSignalDetection) {
      nextSignalDetection.polyline.width = selectedWidth;
    }
  });
}

/**
 * Resets by removing all signal detections when new interval is opened
 *
 * @param dataSource signal detection datasource
 */
export function resetView(dataSource: any) {
  dataSource.entities.removeAll();
}

/**
 * Create new map entities for a list of signal detections
 *
 * @param dataSource signal detection datasource
 * @param signalDetections detections to add
 * @param nextOpenEventId event that is being opened
 */
function addSignalDetections(
  dataSource: any,
  signalDetections: SignalDetectionTypes.SignalDetection[],
  events: EventTypes.Event[],
  currentlyOpenEvent: EventTypes.Event,
  defaultStations: ProcessingStationTypes.ProcessingStation[]
) {
  resetView(dataSource);

  if (currentlyOpenEvent && signalDetections) {
    // Walk through each detection. If the signal detection is unassociated then
    // add it to the data source entities
    signalDetections
      .filter(sd => !sd.currentHypothesis.rejected)
      .forEach(signalDetection => {
        if (isSignalDetectionUnassociated(signalDetection, events)) {
          try {
            // Find the station to get the location for the SD
            const station = defaultStations.find(sta => sta.name === signalDetection.stationName);
            const location = station ? station.location : undefined;
            const openEventHyp = currentlyOpenEvent.currentEventHypothesis.eventHypothesis;
            const sigDetLat = location ? location.latitudeDegrees : undefined;
            const sigDetLon = location ? location.longitudeDegrees : undefined;
            const sigDetAssocLat =
              openEventHyp.preferredLocationSolution.locationSolution.location.latitudeDegrees;
            const sigDetAssocLon =
              openEventHyp.preferredLocationSolution.locationSolution.location.longitudeDegrees;

            dataSource.entities.add({
              name: 'id: ' + signalDetection.id,
              polyline: {
                positions: Cesium.Cartesian3.fromDegreesArray([
                  sigDetLon,
                  sigDetLat,
                  sigDetAssocLon,
                  sigDetAssocLat
                ]),
                width: analystUiConfig.userPreferences.map.widths.unselectedSignalDetection,
                material: Cesium.Color.fromCssColorString(semanticColors.analystUnassociated)
              },
              id: signalDetection.id,
              entityType: 'sd'
            });
          } catch (e) {
            UILogger.Instance().error(`addSignalDetections error: ${e.message}`);
          }
        }
      });
  }
}

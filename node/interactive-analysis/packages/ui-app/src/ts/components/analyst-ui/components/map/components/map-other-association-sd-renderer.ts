import { EventTypes, ProcessingStationTypes, SignalDetectionTypes } from '@gms/common-graphql';
import { UILogger } from '@gms/ui-apollo';
import {
  determineDetectionColor,
  isSignalDetectionOtherAssociated
} from '~analyst-ui/common/utils/signal-detection-util';
import { analystUiConfig } from '~analyst-ui/config';

declare var Cesium;

/**
 * Cesium DataSource with other associated signal detections
 *
 * @param dataSource source of signal detection data
 * @param signalDetections list of map signal detections
 * @param nextOpenEventId id of incoming open event
 */
export function draw(
  dataSource: any,
  signalDetections: SignalDetectionTypes.SignalDetection[],
  events: EventTypes.Event[],
  currentOpenEvent: EventTypes.Event,
  defaultStations: ProcessingStationTypes.ProcessingStation[]
) {
  addOtherAssociatedSignalDetections(
    dataSource,
    signalDetections,
    events,
    currentOpenEvent,
    defaultStations
  );
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
 */
function addOtherAssociatedSignalDetections(
  dataSource: any,
  signalDetections: SignalDetectionTypes.SignalDetection[],
  events: EventTypes.Event[],
  currentOpenEvent: EventTypes.Event,
  defaultStations: ProcessingStationTypes.ProcessingStation[]
) {
  resetView(dataSource);

  if (currentOpenEvent && signalDetections) {
    signalDetections
      .filter(sd => !sd.currentHypothesis.rejected)
      .forEach(signalDetection => {
        // If the signal detection is not associated to the open event
        // and is associated to an other event add it to the data source
        if (isSignalDetectionOtherAssociated(signalDetection, events, currentOpenEvent.id)) {
          try {
            // Find the station to get the location for the SD
            const station = defaultStations.find(sta => sta.name === signalDetection.stationName);
            const location = station ? station.location : undefined;
            const sigDetLat = location ? location.latitudeDegrees : undefined;
            const sigDetLon = location ? location.longitudeDegrees : undefined;

            const currentOpenEventPreferredHypothesis =
              currentOpenEvent.currentEventHypothesis.eventHypothesis;
            const currentLocationSolution =
              currentOpenEventPreferredHypothesis.preferredLocationSolution.locationSolution;
            const sigDetAssocLat = currentLocationSolution.location.latitudeDegrees;
            const sigDetAssocLon = currentLocationSolution.location.longitudeDegrees;

            const color = determineDetectionColor(signalDetection, events, currentOpenEvent.id);
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
                material: Cesium.Color.fromCssColorString(color)
              },
              id: signalDetection.id,
              entityType: 'sd'
            });
          } catch (e) {
            UILogger.Instance().error(`addOtherAssociatedSignalDetections error: ${e.message}`);
          }
        }
      });
  }
}

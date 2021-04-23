/**
 * Interfaces for GMS Map
 * Maintained so that we can switch map libraries if the need arises
 * Currently cesium-map.ts implements this interface
 */

import {
  CommonTypes,
  EventTypes,
  ProcessingStationTypes,
  SignalDetectionTypes
} from '@gms/common-graphql';
import { AnalystUiConfig } from '~analyst-ui/config';
import { MapProps } from '../types';

/**
 * State for the map API
 */
export interface MapAPIState {
  layers: {
    Events: any;
    Stations: any;
    Assoc: any;
    OtherAssoc: any;
    UnAssociated: any;
  };
}
/**
 * Parameters for configuring the map API
 */
export interface MapAPIOptions {
  analystUiConfig: AnalystUiConfig;
  events: {
    onMapClick(e: any, entity?: any): void;
    onMapRightClick(e: any, entity?: any): void;
    onMapShiftClick(e: any, entity?: any): void;
    onMapDoubleClick(e: any, entity?: any): void;
    onMapAltClick(e: any, entity?: any): void;
  };
}

/**
 * Interface/API for GMS Map. Currently, at a minimum a map must implement the methods below
 */
export interface MapAPI {
  getViewer();
  getDataLayers();
  initialize(containerElement: any);
  drawSignalDetections(
    signalDetections: SignalDetectionTypes.SignalDetection[],
    events: EventTypes.Event[],
    nextOpenEvent: EventTypes.Event,
    defaultStations: ProcessingStationTypes.ProcessingStation[]
  );
  drawOtherAssociatedSignalDetections(
    signalDetections: SignalDetectionTypes.SignalDetection[],
    events: EventTypes.Event[],
    currentOpenEvent: EventTypes.Event,
    defaultStations: ProcessingStationTypes.ProcessingStation[]
  );
  drawUnAssociatedSignalDetections(
    signalDetections: SignalDetectionTypes.SignalDetection[],
    events: EventTypes.Event[],
    currentOpenEvent: EventTypes.Event,
    defaultStations: ProcessingStationTypes.ProcessingStation[]
  );
  highlightSelectedSignalDetections(selectedSignalDetection: string[]);
  drawDefaultStations(
    currentStations: ProcessingStationTypes.ProcessingStation[],
    nextStations: ProcessingStationTypes.ProcessingStation[]
  );
  updateStations(
    currentSignalDetections: SignalDetectionTypes.SignalDetection[],
    currentOpenEvent: EventTypes.Event,
    nextSignalDetections: SignalDetectionTypes.SignalDetection[],
    nextOpenEvent: EventTypes.Event
  );
  drawEvents(currenProps: MapProps, nextProps: MapProps);
  highlightOpenEvent(
    currentTimeInterval: CommonTypes.TimeRange,
    currentOpenEvent: EventTypes.Event,
    nextOpenEvent: EventTypes.Event,
    selectedEventIds: string[]
  );
}

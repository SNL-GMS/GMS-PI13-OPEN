import * as Entities from './entities';

/** Constant the number or milliseconds in a second */
export const MILLISECONDS_IN_SECOND = 1000;

/**
 * A constant that defines the weavess label events
 * to all be `undefined`.
 */
export const DEFAULT_UNDEFINED_LABEL_EVENTS: Entities.LabelEvents = {
  onChannelCollapsed: undefined,
  onChannelExpanded: undefined,
  onChannelLabelClick: undefined
};

/**
 * A constant that defines the weavess channel content events
 * to all be `undefined`.
 */
export const DEFAULT_UNDEFINED_CHANNEL_CONTENT_EVENTS: Entities.ChannelContentEvents = {
  onContextMenu: undefined,
  onChannelClick: undefined,
  onSignalDetectionContextMenu: undefined,
  onSignalDetectionClick: undefined,
  onSignalDetectionDragEnd: undefined,
  onPredictivePhaseContextMenu: undefined,
  onPredictivePhaseClick: undefined,
  onPredictivePhaseDragEnd: undefined,
  onMeasureWindowUpdated: undefined,
  onUpdateMarker: undefined,
  onMoveSelectionWindow: undefined,
  onUpdateSelectionWindow: undefined,
  onClickSelectionWindow: undefined
};

/**
 * A constant that defines the weavess events
 * to all be `undefined`.
 */
export const DEFAULT_UNDEFINED_EVENTS: Entities.Events = {
  stationEvents: {
    defaultChannelEvents: {
      labelEvents: DEFAULT_UNDEFINED_LABEL_EVENTS,
      events: DEFAULT_UNDEFINED_CHANNEL_CONTENT_EVENTS,
      onKeyPress: undefined
    },
    nonDefaultChannelEvents: {
      labelEvents: DEFAULT_UNDEFINED_LABEL_EVENTS,
      events: DEFAULT_UNDEFINED_CHANNEL_CONTENT_EVENTS,
      onKeyPress: undefined
    }
  },
  onUpdateMarker: undefined,
  onMoveSelectionWindow: undefined,
  onUpdateSelectionWindow: undefined,
  onClickSelectionWindow: undefined,
  onZoomChange: undefined
};

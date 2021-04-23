import { produce } from 'immer';
import { replaceByIdOrAddToList } from '../../util/common-utils';
import * as model from '../model-and-schema/model';

// TODO add more of these helper functions and replace produce calls with util function calls
// TODO this function should be used in more places as well - this has only been used
// TODO in places where sonarcube was catching duplicate code
/**
 * Updates the current event hypothesis with the passed in hypothesis
 * @param event event to update
 * @param updatedEventHypothesis hypothesis to set as current
 */
export function updateEventCurrentHypothesis(
  event: model.Event,
  updatedEventHypothesis: model.EventHypothesis
) {
  return produce<model.Event>(event, draftState => {
    draftState.hypotheses = replaceByIdOrAddToList<model.EventHypothesis>(
      draftState.hypotheses,
      updatedEventHypothesis
    );
    draftState.currentEventHypothesis.eventHypothesis = updatedEventHypothesis;
  });
}

/**
 * Updates the event hypothesis
 * @param event the event to update
 */
export function updateEventHypothesis(event: model.Event) {
  return produce<model.Event>(event, draftState => {
    draftState.hypotheses = replaceByIdOrAddToList<model.EventHypothesis>(
      draftState.hypotheses,
      event.currentEventHypothesis.eventHypothesis
    );
  });
}

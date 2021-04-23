import { ContextMenu, Menu, MenuItem } from '@blueprintjs/core';
import {
  CommonTypes,
  EventTypes,
  SignalDetectionTypes,
  SignalDetectionUtils
} from '@gms/common-graphql';
import { UILogger } from '@gms/ui-apollo';
import { AnalystWorkspaceTypes } from '@gms/ui-state';
import { OperationVariables } from 'apollo-client';
import Immutable from 'immutable';
import includes from 'lodash/includes';
import React from 'react';
import { MutationFunction } from 'react-apollo';
import { systemConfig } from '~analyst-ui/config';
import { PhaseSelectionMenu } from '../dialogs/phase-selection-menu';
import {
  isAssociatedToCurrentEventHypothesis,
  isInConflict,
  isInConflictAndNotAssociatedToOpenEvent
} from '../utils/signal-detection-util';

/**
 * DetectionRePhaser
 * a callback which executes re-phase logic
 * function to initiate re-phasing
 */
export type DetectionRePhaser = (sdIds: string[], phase: string) => void;

/**
 * DetectionRejecter
 * function to initiate rejecting detection
 */
export type DetectionRejecter = (sdIds: string[]) => void;

/**
 * DetectionFkGenerator
 * function to generate detections for fk's
 */
export type DetectionFkGenerator = () => void;

export type SignalDetectionAssociator = (
  signalDetectionHypoIds: string[],
  eventHypothesisId: string,
  associate: boolean
) => void;

export interface SignalDetectionContextMenuProps {
  signalDetections: SignalDetectionTypes.SignalDetection[];
  selectedSds: SignalDetectionTypes.SignalDetection[];
  sdIdsToShowFk: string[];
  currentOpenEvent: EventTypes.Event;
  changeAssociation: MutationFunction<{}, OperationVariables>;
  associateToNewEvent: MutationFunction<{}, OperationVariables>;
  rejectDetections?: MutationFunction<{}, OperationVariables>;
  updateDetections?: MutationFunction<{}, OperationVariables>;
  measurementMode: AnalystWorkspaceTypes.MeasurementMode;

  setSelectedSdIds(id: string[]): void;
  setSdIdsToShowFk?(signalDetectionIds: string[]): void;
  setMeasurementModeEntries(entries: Immutable.Map<string, boolean>): void;
}
export class SignalDetectionContextMenu extends React.Component<
  SignalDetectionContextMenuProps,
  {}
> {
  private constructor(props) {
    super(props);
  }

  /**
   * React component lifecycle.
   */
  public render() {
    const anyInConflictAndNotAssociatedToOpenEvent: boolean = isInConflictAndNotAssociatedToOpenEvent(
      this.props.selectedSds,
      this.props.currentOpenEvent
    );
    const anyInConflict = isInConflict(this.props.selectedSds);
    const selectedSdIds = this.props.selectedSds.map(sd => sd.id);
    let allRejected = true;
    this.props.selectedSds.forEach(sd => {
      if (!sd.currentHypothesis.rejected) {
        allRejected = false;
      }
    });

    const manualShowMeasurementForSds = [...this.props.measurementMode.entries.entries()]
      .filter(({ 1: v }) => v)
      .map(([k]) => k);

    const manualHideMeasurementForSds = [...this.props.measurementMode.entries.entries()]
      .filter(({ 1: v }) => !v)
      .map(([k]) => k);

    const associatedSignalDetectionHypothesisIds = this.props.currentOpenEvent
      ? this.props.currentOpenEvent.currentEventHypothesis.eventHypothesis.signalDetectionAssociations.map(
          association => association.signalDetectionHypothesis.id
        )
      : [];

    const areAllSelectedAssociatedAndAutoShow =
      this.props.measurementMode.mode === AnalystWorkspaceTypes.WaveformDisplayMode.MEASUREMENT &&
      this.props.selectedSds.every(
        sd =>
          includes(
            systemConfig.measurementMode.phases,
            SignalDetectionUtils.findPhaseFeatureMeasurementValue(
              sd.currentHypothesis.featureMeasurements
            ).phase
          ) &&
          includes(associatedSignalDetectionHypothesisIds, sd.currentHypothesis.id) &&
          !includes(manualHideMeasurementForSds, sd.id)
      );

    const areAllSelectedSdsMarkedAsMeasurementEntriesToShow = this.props.selectedSds.every(sd =>
      includes(manualShowMeasurementForSds, sd.id)
    );

    return (
      <Menu>
        {this.props.updateDetections ? (
          <MenuItem
            text="Set Phase..."
            label={'Ctrl+s'}
            disabled={
              selectedSdIds.length === 0 || allRejected || anyInConflictAndNotAssociatedToOpenEvent
            }
            data-cy="set-phase"
          >
            {setPhaseContextMenu(selectedSdIds, this.props.updateDetections)}
          </MenuItem>
        ) : null}
        <MenuItem text="Event Association" disabled={allRejected} data-cy="association-menu">
          {this.eventAssociationContextMenu(this.props.selectedSds, this.props.currentOpenEvent)}
        </MenuItem>
        {this.props.setSdIdsToShowFk ? (
          <MenuItem
            text="Show FK"
            disabled={
              (selectedSdIds.length > 0 && canDisplayFkForSds(this.props.selectedSds)) ||
              allRejected ||
              anyInConflictAndNotAssociatedToOpenEvent
            }
            onClick={this.setSdIdsToShowFk}
            data-cy="show-fk"
          />
        ) : null}
        {this.props.rejectDetections ? (
          <MenuItem
            text="Reject"
            disabled={selectedSdIds.length === 0 || allRejected || anyInConflict}
            onClick={() => this.rejectDetections(selectedSdIds)}
            data-cy="reject-sd"
          />
        ) : null}
        {this.props.setMeasurementModeEntries ? (
          <MenuItem text="Measure" data-cy="measure">
            <MenuItem
              text={
                areAllSelectedSdsMarkedAsMeasurementEntriesToShow ||
                areAllSelectedAssociatedAndAutoShow
                  ? 'Hide A5/2'
                  : 'Show A5/2'
              }
              onClick={() =>
                this.toggleShownSDs(
                  selectedSdIds,
                  areAllSelectedSdsMarkedAsMeasurementEntriesToShow,
                  areAllSelectedAssociatedAndAutoShow
                )
              }
              data-cy="show-hide-measure"
            />
            <MenuItem
              text={'Hide all A5/2'}
              data-cy="hide-all"
              disabled={
                !(
                  this.props.measurementMode.mode ===
                    AnalystWorkspaceTypes.WaveformDisplayMode.MEASUREMENT ||
                  this.props.measurementMode.entries.size !== 0
                )
              }
              onClick={() => {
                // clear out all the additional measurement mode entries
                let updatedEntries = this.props.measurementMode.entries;
                updatedEntries.forEach(
                  (value, key) => (updatedEntries = updatedEntries.set(key, false))
                );

                if (
                  this.props.measurementMode.mode ===
                  AnalystWorkspaceTypes.WaveformDisplayMode.MEASUREMENT
                ) {
                  // hide all auto show
                  this.props.signalDetections
                    .filter(
                      sd =>
                        includes(associatedSignalDetectionHypothesisIds, sd.currentHypothesis.id) &&
                        includes(
                          systemConfig.measurementMode.phases,
                          SignalDetectionUtils.findPhaseFeatureMeasurementValue(
                            sd.currentHypothesis.featureMeasurements
                          ).phase
                        )
                    )
                    .forEach(sd => (updatedEntries = updatedEntries.set(sd.id, false)));
                }

                this.props.setMeasurementModeEntries(updatedEntries);
              }}
            />

            {this.props.measurementMode.mode ===
            AnalystWorkspaceTypes.WaveformDisplayMode.MEASUREMENT ? (
              <MenuItem
                text={'Show all A5/2 for associated'}
                onClick={() => {
                  // Clear out all the additional measurement mode entries
                  let updatedEntries = this.props.measurementMode.entries;
                  associatedSignalDetectionHypothesisIds.forEach(assocSDHypId => {
                    // Retrieve the SD for the given hypotheses ID
                    const signalDetection = this.props.signalDetections.find(
                      sd => sd.currentHypothesis.id === assocSDHypId
                    );

                    if (
                      includes(
                        systemConfig.measurementMode.phases,
                        SignalDetectionUtils.findPhaseFeatureMeasurementValue(
                          signalDetection.currentHypothesis.featureMeasurements
                        ).phase
                      )
                    ) {
                      updatedEntries = updatedEntries.set(signalDetection.id, true);
                    }
                  });
                  this.props.setMeasurementModeEntries(updatedEntries);
                }}
              />
            ) : (
              undefined
            )}
          </MenuItem>
        ) : (
          undefined
        )}
      </Menu>
    );
  }

  /**
   * Displays a blueprint context menu for event association.
   *
   * @param signalDetections a list of signal detections
   * @returns the event association context menu
   */
  private readonly eventAssociationContextMenu = (
    signalDetections: SignalDetectionTypes.SignalDetection[],
    event: EventTypes.Event
  ): JSX.Element[] => {
    const sdHypotheses = signalDetections.map(sd => sd.currentHypothesis);

    const associatedInList: boolean =
      sdHypotheses.filter(sdHyp => isAssociatedToCurrentEventHypothesis(sdHyp, event)).length > 0;
    const unassociatedInList: boolean =
      sdHypotheses.filter(sdHyp => !isAssociatedToCurrentEventHypothesis(sdHyp, event)).length > 0;
    const menuOptions = [];
    menuOptions.push(
      <MenuItem
        text="Associate to new event"
        onClick={() => {
          this.associateToNewEvent(signalDetections.map(sd => sd.id));
        }}
        data-cy="associate-to-new"
        key="assocnew"
      />
    );
    menuOptions.push(
      associatedInList && unassociatedInList
        ? [
            <MenuItem
              text="Associate to currently open event"
              onClick={() => {
                const sdIdList = signalDetections
                  .filter(sd => !isAssociatedToCurrentEventHypothesis(sd.currentHypothesis, event))
                  .map(sdHyp => sdHyp.id);
                this.unassociateOrAssociateSignalDetections(sdIdList, event, true);
              }}
              disabled={this.props.currentOpenEvent === undefined}
              data-cy="associate-to-open"
              key="assocopen"
            />,
            <MenuItem
              text="Unassociate from currently open event"
              onClick={() => {
                const sdIdList = signalDetections
                  .filter(sd => isAssociatedToCurrentEventHypothesis(sd.currentHypothesis, event))
                  .map(sdHyp => sdHyp.id);
                this.unassociateOrAssociateSignalDetections(sdIdList, event, false);
              }}
              data-cy="unassociate-to-open"
              key="unassocopen"
            />
          ]
        : associatedInList
        ? [
            <MenuItem
              text="Unassociate from currently open event"
              data-cy="unassociate-to-open"
              onClick={() => {
                const sdIdList = signalDetections.map(sd => sd.id);
                this.unassociateOrAssociateSignalDetections(sdIdList, event, false);
              }}
              key="unassocopen"
            />
          ]
        : unassociatedInList
        ? [
            <MenuItem
              text="Associate to currently open event"
              onClick={() => {
                const sdIdList = signalDetections.map(sd => sd.id);
                this.unassociateOrAssociateSignalDetections(sdIdList, event, true);
              }}
              data-cy="associate-to-open"
              disabled={this.props.currentOpenEvent === undefined}
              key="assocopen"
            />
          ]
        : null
    );

    return menuOptions;
  }
  /**
   *
   * show or hide all selected sds
   */
  private readonly toggleShownSDs = (
    selectedSdIds: string[],
    areAllSelectedSdsMarkedAsMeasurementEntriesToShow: boolean,
    areAllSelectedAssociatedAndAutoShow: boolean
  ) => {
    const updatedEntires = {};
    selectedSdIds.forEach(
      id =>
        (updatedEntires[id] = !(
          areAllSelectedSdsMarkedAsMeasurementEntriesToShow || areAllSelectedAssociatedAndAutoShow
        ))
    );
    this.props.setMeasurementModeEntries(this.props.measurementMode.entries.merge(updatedEntires));
  }

  /**
   * Rejects the signal detections for the provided ids.
   *
   * @param sdIds the signal detection ids to reject
   */
  private readonly rejectDetections = (sdIds: string[]) => {
    const input: SignalDetectionTypes.RejectDetectionsMutationArgs = {
      detectionIds: sdIds
    };
    this.props
      .rejectDetections({
        variables: input
      })
      .catch(err => UILogger.Instance().error(`Failed to reject detections: ${err.message}`));
  }

  /**
   * Returns true if the provided signal detection can be used to generate
   * an FK.
   *
   * @param signalDetection the signal detection to check if it can be used to
   * generate an FK.
   * @returns true if the signal detection can be used to generate an FK; false otherwise
   */
  private readonly canGenerateFk = (
    signalDetection: SignalDetectionTypes.SignalDetection
  ): boolean => {
    const fmPhase = SignalDetectionUtils.findPhaseFeatureMeasurementValue(
      signalDetection.currentHypothesis.featureMeasurements
    );
    if (!fmPhase) {
      return false;
    }
    return (
      systemConfig.nonFkSdPhases
        // tslint:disable-next-line:newline-per-chained-call
        .findIndex(phase => phase.toLowerCase() === fmPhase.phase.toString().toLowerCase()) === -1
    );
  }

  /**
   * Sets or updates the signal detection ids to show FK based on
   * the selected signal detections.
   */
  private readonly setSdIdsToShowFk = () => {
    const sdIdsToShowFk = this.props.selectedSds
      .filter(sd => sd && this.canGenerateFk(sd))
      .map(sd => sd.id);
    if (sdIdsToShowFk.length > 0) {
      this.props.setSdIdsToShowFk([...this.props.sdIdsToShowFk, ...sdIdsToShowFk]);
    }
    this.props.setSelectedSdIds(this.props.selectedSds.map(sd => sd.id));
  }

  /**
   * Unassociate or associate the signal detections for the provided event.
   *
   * @param signalDetectionIds the signal detection hypothesis ids
   * @param event the event to unassociate or associate too
   * @param associate boolean flag indicating if we are associating or unassociating
   * to the provided event
   */
  private readonly unassociateOrAssociateSignalDetections = (
    signalDetectionIds: string[],
    event: EventTypes.Event,
    associate: boolean
  ): void => {
    if (!event) {
      return;
    }
    const input: EventTypes.ChangeSignalDetectionAssociationsMutationArgs = {
      eventHypothesisId: event.currentEventHypothesis.eventHypothesis.id,
      signalDetectionIds,
      associate
    };
    this.props
      .changeAssociation({
        variables: input
      })
      .catch(err => UILogger.Instance().error(`Failed to change association: ${err.message}`));
  }
  private readonly associateToNewEvent = (sdIds: string[]) => {
    const input = {
      signalDetectionIds: sdIds
    };
    this.props
      .associateToNewEvent({
        variables: input
      })
      .catch(err => UILogger.Instance().error(`Failed to associate to new event: ${err.message}`));
  }
}

/**
 * Displays a blueprint context menu for selecting a signal detection phase.
 *
 * @param sdIds string array of signal detection ids
 * @param rePhaser graphql mutation that updates a detection
 * @returns the phase selection context menu
 */
export function setPhaseContextMenu(
  sdIds: string[],
  rePhaser: MutationFunction<{}, OperationVariables>
): JSX.Element {
  return (
    <PhaseSelectionMenu
      sdPhases={systemConfig.defaultSdPhases}
      prioritySdPhases={systemConfig.prioritySdPhases}
      onBlur={phase => {
        rePhaseDetections(sdIds, phase, rePhaser);
      }}
      onEnterForPhases={phase => {
        rePhaseDetections(sdIds, phase, rePhaser);
        ContextMenu.hide();
      }}
      onPhaseClicked={phase => {
        rePhaseDetections(sdIds, phase, rePhaser);
        ContextMenu.hide();
      }}
    />
  );
}

/**
 * Returns true if the provided signal detections can be used to display an FK.
 *
 * @param sds a list of signal detections
 * @returns true if the signal detections can be used to display an FK; false otherwise
 */
function canDisplayFkForSds(sds: SignalDetectionTypes.SignalDetection[]): boolean {
  sds.forEach(sd => {
    const fmPhase = SignalDetectionUtils.findPhaseFeatureMeasurementValue(
      sd.currentHypothesis.featureMeasurements
    );
    if (!fmPhase) {
      return false;
    }
    if (
      systemConfig.nonFkSdPhases
        // tslint:disable-next-line:newline-per-chained-call
        .findIndex(phase => phase === fmPhase.phase) < 0
    ) {
      return true;
    }
  });
  return false;
}

/**
 * Rephases the provided signal detection ids.
 *
 * @param phase the signal detection phase to set
 * @param detectionRephaser the mutation for rephasing a signal detection
 */
function rePhaseDetections(
  sdIds: string[],
  phase: CommonTypes.PhaseType,
  detectionRephaser: MutationFunction<{}, OperationVariables>
) {
  const input: SignalDetectionTypes.UpdateDetectionsMutationArgs = {
    detectionIds: sdIds,
    input: {
      phase
    }
  };
  detectionRephaser({
    variables: input
  }).catch(err => UILogger.Instance().error(`Failed to rephase detection: ${err.message}`));
}

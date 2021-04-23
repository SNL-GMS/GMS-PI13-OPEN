import { ContextMenu } from '@blueprintjs/core';
import { EventTypes, SignalDetectionTypes } from '@gms/common-graphql';
import { UILogger } from '@gms/ui-apollo';
import { HorizontalDivider, Toolbar, ToolbarTypes } from '@gms/ui-core-components';
import { Toaster } from '@gms/ui-util';
import cloneDeep from 'lodash/cloneDeep';
import React from 'react';
import { SignalDetectionContextMenu } from '~analyst-ui/common/context-menus';
import { SignalDetectionDetails } from '~analyst-ui/common/dialogs';
import {
  getLatestLocationSolutionSet,
  shouldUpdateSelectedLocationSolution
} from '~analyst-ui/common/utils/event-util';
import {
  getLocationBehavior,
  getNewDefiningForSD,
  getSnapshots,
  initializeSDDiffs,
  removeTypeName,
  updateLocBehaviorFromTableChanges
} from '~analyst-ui/common/utils/location-utils';
import { systemConfig, userPreferences } from '~analyst-ui/config';
import { messageConfig } from '~analyst-ui/config/message-config';
import { semanticColors } from '~scss-config/color-preferences';
import { MAX_DEPTH_KM, MAX_LAT_DEGREES, MAX_LON_DEGREES } from '../constants';
import {
  LocateButtonTooltipMessage,
  LocationPanelProps,
  LocationPanelState,
  SignalDetectionTableRowChanges
} from '../types';
import { LocationHistory } from './location-history';
import { LocationSignalDetections } from './location-signal-detections';
import { DefiningChange, DefiningTypes } from './location-signal-detections/types';

export class LocationPanel extends React.Component<LocationPanelProps, LocationPanelState> {
  // Toaster for user feedback
  private static readonly toaster: Toaster = new Toaster();

  /**
   * constructor
   */
  public constructor(props: LocationPanelProps) {
    super(props);
    this.state = {
      outstandingLocateCall: false,
      sdDefiningChanges: initializeSDDiffs(props.signalDetectionsByStation)
    };
  }

  // ***************************************
  // BEGIN REACT COMPONENT LIFECYCLE METHODS
  // ***************************************

  /**
   * React component lifecycle
   *
   * @param prevProps The previous properties available to this react component
   */
  public componentDidUpdate(prevProps: LocationPanelProps) {
    // If new time interval, setup subscriptions

    // If the open event has changed, if a new locate has come in,
    // or if the event hyp has changed - update the state
    if (
      this.props.openEvent.id !== prevProps.openEvent.id ||
      shouldUpdateSelectedLocationSolution(prevProps.openEvent, this.props.openEvent)
    ) {
      const currentLSS = getLatestLocationSolutionSet(this.props.openEvent);
      if (currentLSS) {
        this.props.setSelectedLocationSolution(currentLSS.id, currentLSS.locationSolutions[0].id);
      }

      this.setState({
        sdDefiningChanges: initializeSDDiffs(this.props.signalDetectionsByStation)
      });
    }
  }

  public render() {
    const height = 200;
    const snapshots = getSnapshots(
      this.isViewingPreviousLocation(),
      this.props.openEvent,
      this.props.location.selectedLocationSolutionSetId,
      this.props.location.selectedLocationSolutionId,
      this.state.sdDefiningChanges,
      this.props.associatedSignalDetections
    );

    const disableLocate: {
      isDisabled: boolean;
      reason: LocateButtonTooltipMessage | string;
    } = this.disableLocate();
    const toolbarItems: ToolbarTypes.ToolbarItem[] = [];
    const locateButton: ToolbarTypes.ButtonItem = {
      disabled: disableLocate.isDisabled,
      label: 'Locate',
      tooltip: disableLocate.reason,
      rank: 1,
      onClick: () => {
        this.locate();
      },
      widthPx: 60,
      type: ToolbarTypes.ToolbarItemType.Button,
      cyData: 'location-locate-button'
    };
    toolbarItems.push(locateButton);
    const toolbarLeftItems: ToolbarTypes.ToolbarItem[] = [];
    const locateSpinner: ToolbarTypes.LoadingSpinnerItem = {
      tooltip: messageConfig.tooltipMessages.location.locateCallInProgressMessage,
      label: 'Locating',
      type: ToolbarTypes.ToolbarItemType.LoadingSpinner,
      rank: 1,
      itemsToLoad: this.state.outstandingLocateCall ? 1 : 0,
      hideTheWordLoading: true,
      hideOutstandingCount: false,
      widthPx: 100
    };
    toolbarLeftItems.push(locateSpinner);

    return (
      <div
        className="location-wrapper"
        onKeyDown={this.onKeyPress}
        tabIndex={-1}
        data-cy="location"
        onMouseEnter={e => {
          e.currentTarget.focus();
        }}
      >
        <Toolbar
          items={toolbarItems}
          itemsLeft={toolbarLeftItems}
          toolbarWidthPx={this.props.widthOfDisplayPx - userPreferences.list.widthOfTableMarginsPx}
        />
        <HorizontalDivider
          topHeightPx={height}
          top={
            <LocationHistory
              event={this.props.openEvent}
              location={this.props.location}
              setSelectedLocationSolution={this.props.setSelectedLocationSolution}
              setSelectedPreferredLocationSolution={this.props.setSelectedPreferredLocationSolution}
            />
          }
          bottom={
            <LocationSignalDetections
              event={this.props.openEvent}
              distances={this.props.distances}
              signalDetectionDiffSnapshots={snapshots}
              historicalMode={this.isViewingPreviousLocation()}
              changeSignalDetectionAssociations={this.props.changeSignalDetectionAssociations}
              rejectDetections={this.props.rejectDetections}
              updateDetections={this.props.updateDetections}
              createEvent={this.props.createEvent}
              setSdIdsToShowFk={sdIds => {
                this.props.setSdIdsToShowFk(sdIds);
              }}
              selectedSdIds={this.props.selectedSdIds}
              setSelectedSdIds={sdIds => {
                this.props.setSelectedSdIds(sdIds);
              }}
              showSDContextMenu={this.showSDContextMenu}
              showSDDetails={this.showSDDetails}
              updateIsDefining={this.updateIsDefining}
              setDefining={this.setDefiningForColumn}
              setMeasurementModeEntries={this.props.setMeasurementModeEntries}
              toast={message => {
                LocationPanel.toaster.toastInfo(message);
              }}
            />
          }
        />
      </div>
    );
  }

  // ***************************************
  // END REACT COMPONENT LIFECYCLE METHODS
  // ***************************************

  private readonly onKeyPress = (e: React.KeyboardEvent<HTMLDivElement>) => {
    if (e.metaKey || e.ctrlKey) {
      if (e.nativeEvent.code === 'KeyL') {
        if (!this.disableLocate()) {
          this.locate();
        }
      }
    }
  }

  private readonly isViewingPreviousLocation = (): boolean => {
    const latestLocationSS = getLatestLocationSolutionSet(this.props.openEvent);
    return (
      latestLocationSS && latestLocationSS.id !== this.props.location.selectedLocationSolutionSetId
    );
  }

  /**
   * Event Handlers
   */
  private readonly showSDContextMenu = (selectedSdIds: string[], x: number, y: number) => {
    const sds: SignalDetectionTypes.SignalDetection[] = this.props.signalDetectionsByStation.filter(
      sd => selectedSdIds.indexOf(sd.id) >= 0
    );
    const context = (
      <SignalDetectionContextMenu
        signalDetections={this.props.signalDetectionsByStation}
        selectedSds={sds}
        sdIdsToShowFk={this.props.sdIdsToShowFk}
        currentOpenEvent={this.props.openEvent}
        changeAssociation={this.props.changeSignalDetectionAssociations}
        rejectDetections={this.props.rejectDetections}
        updateDetections={this.props.updateDetections}
        setSdIdsToShowFk={this.props.setSdIdsToShowFk}
        associateToNewEvent={this.props.createEvent}
        measurementMode={this.props.measurementMode}
        setSelectedSdIds={this.props.setSelectedSdIds}
        setMeasurementModeEntries={this.props.setMeasurementModeEntries}
      />
    );
    ContextMenu.show(
      context,
      {
        left: x,
        top: y
      },
      undefined,
      true
    );
  }

  /**
   * Shows signal detection details
   *
   * @param sdId Id of signal detection
   * @param x offset left for context menu
   * @param y offset top for context menu
   */
  private readonly showSDDetails = (sdId: string, x: number, y: number) => {
    // Display information of the signal detection
    const detection = this.props.signalDetectionsByStation.filter(sd => sd.id === sdId);
    ContextMenu.show(
      <SignalDetectionDetails detection={detection[0]} color={semanticColors.analystOpenEvent} />,
      { left: x, top: y },
      undefined,
      true
    );
  }

  /**
   * Update the Signal Detection isDefining value(checkbox) in the state
   *
   * @param definingType which isDefing value to update Arrival Time, Slowness or Azimuth
   * @param signalDetectionHypothesisId which Signal Detection hypothesis to update
   * @param setDefining if true sets defining to true, otherwise false
   */
  private readonly updateIsDefining = (
    definingType: DefiningTypes,
    signalDetectionId: string,
    setDefining: boolean
  ): void => {
    const signalDetection = this.props.associatedSignalDetections.find(
      sd => sd.id === signalDetectionId
    );
    // If we do not find the signal detection then can't update
    if (!signalDetection) {
      return;
    }
    const sdRowChanges: SignalDetectionTableRowChanges = this.state.sdDefiningChanges.find(
      row => row.signalDetectionId === signalDetection.id
    )
      ? this.state.sdDefiningChanges.find(row => row.signalDetectionId === signalDetection.id)
      : {
          arrivalTimeDefining: DefiningChange.NO_CHANGE,
          azimuthDefining: DefiningChange.NO_CHANGE,
          slownessDefining: DefiningChange.NO_CHANGE,
          signalDetectionId: signalDetection.id
        };
    const newSdRow = getNewDefiningForSD(
      definingType,
      setDefining,
      signalDetection,
      sdRowChanges,
      this.props.openEvent
    );
    const newDefining = [
      ...this.state.sdDefiningChanges.filter(sdc => sdc.signalDetectionId !== signalDetection.id),
      newSdRow
    ];
    this.setState({ sdDefiningChanges: newDefining });
  }

  /**
   * Sets new sd rows to state
   *
   * @param isDefining whether the new row is defining
   * @param definingType which fm will be set
   */
  private readonly setDefiningForColumn = (isDefining: boolean, definingType: DefiningTypes) => {
    const associatedSignalDetections = this.props.associatedSignalDetections;
    const currentSdIds = associatedSignalDetections.map(sd => sd.id);
    const rowsWithNullEntriesFilled = currentSdIds.map(sdId =>
      this.state.sdDefiningChanges.find(sdc => sdc.signalDetectionId === sdId)
        ? this.state.sdDefiningChanges.find(sdc => sdc.signalDetectionId === sdId)
        : {
            arrivalTimeDefining: DefiningChange.NO_CHANGE,
            azimuthDefining: DefiningChange.NO_CHANGE,
            slownessDefining: DefiningChange.NO_CHANGE,
            signalDetectionId: sdId
          }
    );
    const newRows = rowsWithNullEntriesFilled.map(row =>
      getNewDefiningForSD(
        definingType,
        isDefining,
        associatedSignalDetections.find(sd => sd.id === row.signalDetectionId),
        row,
        this.props.openEvent
      )
    );
    this.setState({
      ...this.state,
      sdDefiningChanges: newRows
    });
  }

  // ***************************************
  // BEGIN Helper functions, please move to a util when possible
  // ***************************************

  /**
   * Determines if a location is valid based on its depth, lat, and long
   */
  private readonly isLocationValid = (location: EventTypes.LocationSolution): boolean => {
    let valid = true;
    valid = valid && location.location.depthKm <= MAX_DEPTH_KM && location.location.depthKm >= 0;
    valid = valid && Math.abs(location.location.latitudeDegrees) <= MAX_LAT_DEGREES;
    valid = valid && Math.abs(location.location.longitudeDegrees) <= MAX_LON_DEGREES;
    return valid;
  }

  /**
   * Returns true is the latest calculated location set has valid date
   */
  private readonly isLastLocationSetValid = (): boolean => {
    const preferred = getLatestLocationSolutionSet(this.props.openEvent);
    let valid = true;
    if (preferred) {
      preferred.locationSolutions.forEach(l => {
        valid = valid && this.isLocationValid(l);
      });
    }
    return valid;
  }

  /**
   * Determines if the locate button can be used
   */
  private readonly disableLocate = (): {
    isDisabled: boolean;
    reason: LocateButtonTooltipMessage | string;
  } => {
    if (!this.isLastLocationSetValid()) {
      return {
        isDisabled: true,
        reason: LocateButtonTooltipMessage.BadLocationAttributes
      };
    }

    const numberOfRequiredBehaviors =
      systemConfig.numberOfDefiningLocationBehaviorsRequiredForLocate;
    const locationBehaviors = this.getLocationBehaviors();
    const definingList = locationBehaviors.map(lb => lb.defining);
    const definingCount = definingList.reduce((prev, cur) => (cur ? prev + 1 : prev), 0);
    return definingCount < systemConfig.numberOfDefiningLocationBehaviorsRequiredForLocate
      ? {
          isDisabled: true,
          reason: `${numberOfRequiredBehaviors} ${LocateButtonTooltipMessage.NotEnoughDefiningBehaviors}`
        }
      : { isDisabled: false, reason: LocateButtonTooltipMessage.Correct };
  }

  /**
   * Sends location mutation to the gateway
   */
  private readonly locate = () => {
    const eventHypothesisId = this.props.openEvent.currentEventHypothesis.eventHypothesis.id;
    const locationBehaviors = this.getLocationBehaviors();
    // Call the mutation the return is the updated EventHypothesis
    // which magically updates in Apollo cache
    const variables: EventTypes.LocateEventMutationArgs = {
      eventHypothesisId,
      preferredLocationSolutionId: this.props.location.selectedPreferredLocationSolutionId,
      locationBehaviors
    };
    this.setState({ outstandingLocateCall: true });
    this.props
      .locateEvent({
        variables
      })
      .then(() => {
        this.setState({ outstandingLocateCall: false });
      })
      .catch(e => UILogger.Instance().error(`Failed to locate: ${e.message}`));
  }

  /**
   * Retrieve the Location Behaviors from the current SignalDetectionAssociations.
   * Used by Locate Event Mutation
   *
   * @param signalDetections list of sd's to get location behaviors from
   * @returns List of LocationBehaviors
   */
  private readonly getLocationBehaviors = (): EventTypes.LocationBehavior[] => {
    const locationBehaviors: EventTypes.LocationBehavior[] = [];
    // For each SD find the SD Row for the defining values.
    // Change the location behavior defining values according.
    const snaps = getSnapshots(
      this.isViewingPreviousLocation(),
      this.props.openEvent,
      this.props.location.selectedLocationSolutionSetId,
      this.props.location.selectedLocationSolutionId,
      this.state.sdDefiningChanges,
      this.props.associatedSignalDetections
    );
    // tslint:disable-next-line: cyclomatic-complexity
    snaps.forEach(sdsnap => {
      if (!sdsnap.rejectedOrUnassociated) {
        const prevLocationBehaviors = cloneDeep(
          this.props.openEvent.currentEventHypothesis.eventHypothesis.preferredLocationSolution
            .locationSolution.locationBehaviors
        );
        const sdTableRowChange = this.state.sdDefiningChanges.find(
          sdRC => sdRC.signalDetectionId === sdsnap.signalDetectionId
        );
        const sd = this.props.signalDetectionsByStation.find(
          sdreal => sdreal.id === sdsnap.signalDetectionId
        );
        const arrivalLoc = getLocationBehavior(
          DefiningTypes.ARRIVAL_TIME,
          sd,
          prevLocationBehaviors
        );
        const azimuthLoc = getLocationBehavior(DefiningTypes.AZIMUTH, sd, prevLocationBehaviors);
        const slowLoc = getLocationBehavior(DefiningTypes.SLOWNESS, sd, prevLocationBehaviors);

        if (arrivalLoc) {
          const newArrivalBehavior = sdTableRowChange
            ? updateLocBehaviorFromTableChanges(arrivalLoc, sdTableRowChange, 'arrivalTimeDefining')
            : arrivalLoc;
          locationBehaviors.push(removeTypeName(newArrivalBehavior));
        }
        if (azimuthLoc) {
          const newAzimuthBehavior = sdTableRowChange
            ? updateLocBehaviorFromTableChanges(azimuthLoc, sdTableRowChange, 'azimuthDefining')
            : arrivalLoc;
          locationBehaviors.push(removeTypeName(newAzimuthBehavior));
        }
        if (slowLoc) {
          const newSlownessBehavior = sdTableRowChange
            ? updateLocBehaviorFromTableChanges(slowLoc, sdTableRowChange, 'slownessDefining')
            : arrivalLoc;
          locationBehaviors.push(removeTypeName(newSlownessBehavior));
        }
      }
    });
    return locationBehaviors;
  }
}

import { Intent, Spinner } from '@blueprintjs/core';
import { IconNames } from '@blueprintjs/icons';
import { CommonTypes } from '@gms/common-graphql';
import { Toolbar, ToolbarTypes } from '@gms/ui-core-components';
import { AnalystWorkspaceTypes } from '@gms/ui-state';
import defer from 'lodash/defer';
import intersection from 'lodash/intersection';
import React from 'react';
import { AlignmentMenu, PhaseSelectionMenu, QcMaskFilter } from '~analyst-ui/common/dialogs';
import { analystUiConfig } from '~analyst-ui/config';
import { AlignWaveformsOn, PanType } from '../../types';
import { DEFAULT_INITIAL_WAVEFORM_CLIENT_STATE } from '../../waveform-client/constants';
import { WaveformDisplayControlsProps, WaveformDisplayControlsState } from './types';

const GL_CONTAINER_PADDING_PX = 16;
/**
 *  Waveform Display Controls Component
 */
export class WaveformDisplayControls extends React.Component<
  WaveformDisplayControlsProps,
  WaveformDisplayControlsState
> {
  /**
   * The rank of the alignment popover
   */
  private RANK_OF_ALIGNMENT_POPOVER: number = 0;

  /**
   * handle to the alignment button
   */
  private toolbarRef: Toolbar;

  public constructor(props: WaveformDisplayControlsProps) {
    super(props);
    this.state = {
      hasMounted: false,
      waveformState: {
        ...DEFAULT_INITIAL_WAVEFORM_CLIENT_STATE
      }
    };
  }

  public componentDidMount() {
    this.setState({ hasMounted: true });
  }

  /**
   * React component lifecycle
   */
  public render() {
    const defaultSdPhasesList = analystUiConfig.systemConfig.defaultSdPhases;
    const prioritySdPhasesList = analystUiConfig.systemConfig.prioritySdPhases;

    let rank = 1;

    const leftToolbarLeftItems: ToolbarTypes.ToolbarItem[] = [];
    const modeSelector: ToolbarTypes.DropdownItem = {
      label: 'Mode',
      tooltip: 'Set the display mode',
      type: ToolbarTypes.ToolbarItemType.Dropdown,
      value: this.props.measurementMode.mode,
      disabled:
        this.props.currentOpenEventId === null ||
        this.props.currentOpenEventId === undefined ||
        this.props.currentOpenEventId === '',
      rank: rank++,
      onChange: value => {
        this.props.setMode(value);
      },
      dropdownOptions: AnalystWorkspaceTypes.WaveformDisplayMode,
      widthPx: 130
    };
    leftToolbarLeftItems.push(modeSelector);
    const rightToolbarItemDefs: ToolbarTypes.ToolbarItem[] = [];
    const phaseSelectionDropDown = (
      <PhaseSelectionMenu
        phase={this.props.defaultSignalDetectionPhase}
        sdPhases={defaultSdPhasesList}
        prioritySdPhases={prioritySdPhasesList}
        onBlur={() => {
          return;
        }}
        onEnterForPhases={phase => {
          this.hideToolbarPopover();
          this.props.setDefaultSignalDetectionPhase(phase);
        }}
        onPhaseClicked={phase => {
          this.hideToolbarPopover();
          this.props.setDefaultSignalDetectionPhase(phase);
        }}
      />
    );
    const sdPhaseSelector: ToolbarTypes.PopoverItem = {
      label: this.props.defaultSignalDetectionPhase,
      menuLabel: 'Default Phase',
      tooltip: 'Set default phase of new signal detections',
      type: ToolbarTypes.ToolbarItemType.Popover,
      rank: rank++,
      popoverContent: phaseSelectionDropDown,
      widthPx: 88,
      onPopoverDismissed: () => {
        return;
      }
    };
    rightToolbarItemDefs.push(sdPhaseSelector);
    const visibleWaveforms: ToolbarTypes.NumericInputItem = {
      label: 'Visible Waveforms',
      labelRight: 'per screen',
      tooltip: 'Sets the number of visible waveforms per screen',
      type: ToolbarTypes.ToolbarItemType.NumericInput,
      rank: rank++,
      onChange: value => this.props.setAnalystNumberOfWaveforms(value),
      value: this.props.analystNumberOfWaveforms,
      minMax: {
        min: 1,
        max: 100
      }
    };
    rightToolbarItemDefs.push(visibleWaveforms);
    const alignmentDropdown = (
      <AlignmentMenu
        alignedOn={this.props.alignWaveformsOn}
        sdPhases={
          this.props.alignablePhases
            ? this.alignablePhasesUnion(defaultSdPhasesList)
            : defaultSdPhasesList
        }
        phaseAlignedOn={this.props.phaseToAlignOn}
        prioritySdPhases={
          this.props.alignablePhases
            ? this.alignablePhasesUnion(prioritySdPhasesList)
            : prioritySdPhasesList
        }
        onSubmit={(alignedOn: AlignWaveformsOn, sdPhase?: CommonTypes.PhaseType) => {
          this.hideToolbarPopover();
          this.props.setWaveformAlignment(
            alignedOn,
            sdPhase,
            alignedOn !== AlignWaveformsOn.TIME ? true : this.props.showPredictedPhases
          );
        }}
      />
    );
    this.RANK_OF_ALIGNMENT_POPOVER = rank++;
    const alignmentLabel =
      this.props.alignWaveformsOn === AlignWaveformsOn.TIME
        ? 'Time'
        : `${this.props.alignWaveformsOn} ${this.props.phaseToAlignOn}`;
    const alignmentSelector: ToolbarTypes.PopoverItem = {
      label: alignmentLabel,
      tooltip: 'Align waveforms to time or phase',
      type: ToolbarTypes.ToolbarItemType.Popover,
      menuLabel: 'Alignment',
      disabled:
        this.props.currentOpenEventId === null ||
        this.props.currentOpenEventId === undefined ||
        this.props.currentOpenEventId === '',
      rank: this.RANK_OF_ALIGNMENT_POPOVER,
      popoverContent: alignmentDropdown,
      widthPx: 154,
      onPopoverDismissed: () => {
        return;
      }
    };
    rightToolbarItemDefs.push(alignmentSelector);
    const stationSort: ToolbarTypes.DropdownItem = {
      label: 'Station Sort',
      tooltip: 'Set the sort order of stations',
      type: ToolbarTypes.ToolbarItemType.Dropdown,
      value: this.props.currentSortType,
      disabled:
        this.props.alignWaveformsOn !== AlignWaveformsOn.TIME || !this.props.currentOpenEventId,
      rank: rank++,
      onChange: value => {
        this.props.setSelectedSortType(value);
      },
      dropdownOptions: AnalystWorkspaceTypes.WaveformSortType,
      widthPx: 130
    };
    rightToolbarItemDefs.push(stationSort);
    const predictedDropdown: ToolbarTypes.SwitchItem = {
      label: 'Predicted Phases',
      tooltip: 'Show/Hide predicted phases',
      rank: rank++,
      onChange: val => this.props.setShowPredictedPhases(val),
      type: ToolbarTypes.ToolbarItemType.Switch,
      value: this.props.showPredictedPhases,
      menuLabel: this.props.showPredictedPhases ? 'Hide Predicted Phase' : 'Show Predicted Phases',
      cyData: 'Predicted Phases'
    };
    rightToolbarItemDefs.push(predictedDropdown);
    const qcMaskPicker: ToolbarTypes.PopoverItem = {
      label: 'QC Masks',
      tooltip: 'Show/Hide categories of QC masks',
      rank: rank++,
      widthPx: 110,
      onPopoverDismissed: () => {
        return;
      },
      type: ToolbarTypes.ToolbarItemType.Popover,
      popoverContent: (
        <QcMaskFilter
          maskDisplayFilters={this.props.maskDisplayFilters}
          setMaskDisplayFilters={this.props.setMaskDisplayFilters}
        />
      )
    };
    rightToolbarItemDefs.push(qcMaskPicker);
    const measureWindowSwitch: ToolbarTypes.SwitchItem = {
      label: 'Measure Window',
      tooltip: 'Show/Hide Measure Window',
      type: ToolbarTypes.ToolbarItemType.Switch,
      value: this.props.isMeasureWindowVisible,
      rank: rank++,
      onChange: e => this.props.toggleMeasureWindow(),
      menuLabel: this.props.isMeasureWindowVisible ? 'Hide Measure Window' : 'Show Measure Window'
    };
    rightToolbarItemDefs.push(measureWindowSwitch);
    const panGroup: ToolbarTypes.ButtonGroupItem = {
      buttons: [
        {
          label: 'Pan Left',
          tooltip: 'Pan waveforms to the left',
          type: ToolbarTypes.ToolbarItemType.Button,
          rank: rank++,
          icon: IconNames.ARROW_LEFT,
          onlyShowIcon: true,
          onClick: () => this.props.pan(PanType.Left)
        },
        {
          label: 'Pan Right',
          tooltip: 'Pan waveforms to the Right',
          type: ToolbarTypes.ToolbarItemType.Button,
          rank: rank++,
          icon: IconNames.ARROW_RIGHT,
          onlyShowIcon: true,
          onClick: () => this.props.pan(PanType.Right)
        }
      ],
      label: 'Pan',
      tooltip: '',
      type: ToolbarTypes.ToolbarItemType.ButtonGroup,
      rank
    };
    rightToolbarItemDefs.push(panGroup);

    return (
      <div className={'waveform-display-control-pannel'}>
        <div className={'waveform-display-control-pannel__status'}>
          <div className="waveform-display-controls-status">
            {this.state.waveformState.isLoading ? (
              <Spinner
                intent={Intent.PRIMARY}
                size={Spinner.SIZE_SMALL}
                value={this.state.waveformState.percent}
              />
            ) : (
              undefined
            )}
            {this.state.waveformState.isLoading ? (
              <span>{this.state.waveformState.description}</span>
            ) : (
              undefined
            )}
          </div>
        </div>
        <Toolbar
          itemsLeft={leftToolbarLeftItems}
          items={rightToolbarItemDefs}
          ref={ref => {
            if (ref) {
              this.toolbarRef = ref;
            }
          }}
          // ! TODO DO NOT USE `this.props.glContainer.width` TO CALCULATING WIDTH - COMPONENT MAY NOT BE INSIDE GL
          toolbarWidthPx={
            this.props.glContainer ? this.props.glContainer.width - GL_CONTAINER_PADDING_PX : 0
          }
        />
      </div>
    );
  }

  /**
   * Toggles the alignment dropdown
   */
  public readonly toggleAlignmentDropdown = () => {
    if (this.toolbarRef) {
      this.toolbarRef.togglePopover(this.RANK_OF_ALIGNMENT_POPOVER);
    }
  }

  /**
   * Hides the toolbar popover.
   */
  private readonly hideToolbarPopover = () => {
    if (this.toolbarRef) {
      document.addEventListener('dblclick', this.preventDoubleClick, { capture: true });
      this.toolbarRef.hidePopup();
      defer(() =>
        document.removeEventListener('dblclick', this.preventDoubleClick, { capture: true })
      );
    }
  }

  /**
   * Prevents a double click event.
   */
  private readonly preventDoubleClick = (event: Event) => {
    event.preventDefault();
    event.stopPropagation();
  }

  /**
   * Returns the alignable phases.
   */
  private readonly alignablePhasesUnion = (defaultList: CommonTypes.PhaseType[]) => {
    const unionResult = intersection(this.props.alignablePhases, defaultList);
    return unionResult;
  }
}

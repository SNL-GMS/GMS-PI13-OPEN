import { Button, Card, Dialog } from '@blueprintjs/core';
import { SystemMessageTypes, UserProfileTypes } from '@gms/common-graphql';
import { UILogger } from '@gms/ui-apollo';
import { InfiniteTable, useDatasource } from '@gms/ui-core-components';
import classNames from 'classnames';
import includes from 'lodash/includes';
import React, { useState } from 'react';
import { userPreferences } from '~components/common-ui/config/user-preferences';
import {
  getHeaderHeight,
  getRowHeightWithBorder
} from '~components/data-acquisition-ui/shared/table/utils';
import { AudibleNotificationContext } from '../audio/audible-notification-context';
import { defaultColumnDefinition } from './sound-configuration-cell-renderer';
import { columnDefs } from './sound-configuration-column-defs';
import { SoundConfigurationToolbar } from './sound-configuration-toolbar';
import { filterRowsByType, getAvailableSounds, getMissingSounds } from './sound-configuration-util';
import { SoundSample } from './sound-sample';
import {
  ALL_CATEGORIES,
  ALL_SEVERITIES,
  ALL_SUBCATEGORIES,
  FILTER_TYPE,
  SelectedOptions as SelectedFilterOptions,
  SoundConfigurationRow
} from './types';

interface SoundConfigurationProps {
  isVisible: boolean;
  systemMessageDefinitions: SystemMessageTypes.SystemMessageDefinition[];
  onToggle(): void;
}

/**
 * Custom hook to manage which rows are filtered and which are not.
 * @param rows the rows to filter
 * @returns
 * an the filtered rows;
 * a callback function to call when a filter is changed;
 * and a SelectedFilterOptions object containing the state of each filter
 */
const useRowFilters = (
  rows: SoundConfigurationRow[]
): [SoundConfigurationRow[], (type: FILTER_TYPE, value: any) => void, SelectedFilterOptions] => {
  const [selectedSeverity, setSelectedSeverity] = useState(ALL_SEVERITIES);
  const [selectedCategory, setSelectedCategory] = useState(ALL_CATEGORIES);
  const [selectedSubcategory, setSelectedSubcategory] = useState(ALL_SUBCATEGORIES);

  const onFilterChanged = (type: FILTER_TYPE, value): void => {
    switch (type) {
      case FILTER_TYPE.SEVERITY:
        setSelectedSeverity(value);
        break;
      case FILTER_TYPE.CATEGORY:
        setSelectedCategory(value);
        break;
      case FILTER_TYPE.SUBCATEGORY:
        setSelectedSubcategory(value);
        break;
      default:
        // This should never happen...
        // tslint:disable-next-line: no-console
        console.error('Unknown SystemMessage category');
    }
  };

  const filteredRows = filterRowsByType(
    selectedSeverity,
    rows,
    selectedCategory,
    selectedSubcategory
  );
  return [
    filteredRows,
    onFilterChanged,
    {
      selectedSeverity,
      selectedCategory,
      selectedSubcategory
    }
  ];
};

/**
 * creates a list of sound configuration rows.
 * @param systemMessageDefinitions the defined system messages from which to generate rows
 * @param onSelect called when a selection is made.
 */
const useSoundConfigurationRows = (
  systemMessageDefinitions: SystemMessageTypes.SystemMessageDefinition[],
  tableRef: React.MutableRefObject<InfiniteTable<SoundConfigurationRow, {}>>,
  onSelect?: (fileName: string) => void
): SoundConfigurationRow[] => {
  const context = React.useContext(AudibleNotificationContext);

  const [missingSoundFiles, setMissingSoundFiles] = useState<string[]>(null);

  const configuredAudibleNotifications = context.audibleNotifications?.map(a => a.fileName);

  React.useEffect(() => {
    getMissingSounds(configuredAudibleNotifications)
      .then(response => {
        setMissingSoundFiles(response);
      })
      .catch(error => UILogger.Instance().error(error));
  }, [context]);

  const availableSounds = getAvailableSounds();

  const getSelectedSound = (
    systemMessageDefinition: SystemMessageTypes.SystemMessageDefinition
  ): string =>
    context.audibleNotifications?.find(
      notification =>
        SystemMessageTypes.SystemMessageType[notification.notificationType] ===
        SystemMessageTypes.SystemMessageType[systemMessageDefinition.systemMessageType]
    )?.fileName ?? 'None';

  const rows: SoundConfigurationRow[] = systemMessageDefinitions.map(
    (systemMessageDefinition, idx) => ({
      id: `${idx}`,
      sound: {
        availableSounds,
        selectedSound: getSelectedSound(systemMessageDefinition),
        onSelect: selectedFileName => {
          const notification: UserProfileTypes.AudibleNotification = {
            fileName: selectedFileName === 'None' ? '' : selectedFileName,
            notificationType:
              SystemMessageTypes.SystemMessageType[systemMessageDefinition.systemMessageType]
          };

          context
            .setAudibleNotifications([notification])
            .then(result => {
              onSelect(selectedFileName);
              const tableApi = tableRef.current.getTableApi();
              tableApi.refreshInfiniteCache();
            })
            .catch(err => {
              UILogger.Instance().error(err);
            });
        }
      },
      hasNotificationStatusError:
        missingSoundFiles &&
        missingSoundFiles.length > 0 &&
        includes(missingSoundFiles, getSelectedSound(systemMessageDefinition)),
      category: systemMessageDefinition.systemMessageCategory,
      subcategory: systemMessageDefinition.systemMessageSubCategory,
      severity: systemMessageDefinition.systemMessageSeverity,
      message: systemMessageDefinition.template
    })
  );

  return rows;
};

/**
 * The actual component that creates a dialog, chooses and plays sounds.
 * Calls on the context.setAudibleNotifications to update the user profile as
 * soon as user selects a sound.
 */
const SoundConfigurationComponent: React.FunctionComponent<SoundConfigurationProps> = ({
  isVisible,
  onToggle,
  systemMessageDefinitions
}) => {
  const [sampleSound, setSampleSound] = useState<string>(null);
  const tableRef = React.useRef<InfiniteTable<SoundConfigurationRow, {}>>(null);
  const setSampleSoundWithPath = (s: string) =>
    setSampleSound(`${userPreferences.baseSoundsPath}${s}`);
  const rows = useSoundConfigurationRows(
    systemMessageDefinitions,
    tableRef,
    setSampleSoundWithPath
  );
  const [filteredRows, onFilterChanged, selectedOptions] = useRowFilters(rows);
  const datasource = useDatasource(filteredRows);

  return (
    <Dialog
      onClose={onToggle}
      isOpen={isVisible}
      autoFocus={true}
      hasBackdrop={true}
      canEscapeKeyClose={true}
      title={'Sound Configuration'}
      style={{ width: 'auto' }}
      canOutsideClickClose={true}
    >
      <div className={classNames('sound-configuration ag-theme-dark')}>
        <SoundSample soundToPlay={sampleSound} />
        <Card interactive={false}>
          <SoundConfigurationToolbar
            systemMessageDefinitions={systemMessageDefinitions}
            selectedOptions={selectedOptions}
            onChanged={onFilterChanged}
          />
          <div className={'sound-configuration__container'}>
            <InfiniteTable<SoundConfigurationRow, {}>
              ref={ref => (tableRef.current = ref)}
              id="table-sound-configuration"
              key="table-sound-configuration"
              datasource={datasource}
              context={{}}
              defaultColDef={defaultColumnDefinition}
              columnDefs={columnDefs}
              rowData={filteredRows}
              rowHeight={getRowHeightWithBorder()}
              headerHeight={getHeaderHeight()}
              getRowNodeId={node => node.id}
              deltaRowDataMode={true}
              rowDeselection={true}
              overlayNoRowsTemplate="No sounds to display"
              suppressContextMenu={true}
            />
          </div>
          <div className={'sound-configuration__footer'}>
            <Button className={'sound-configuration__close-button'} onClick={onToggle}>
              Close
            </Button>
          </div>
        </Card>
      </div>
    </Dialog>
  );
};

/**
 * Dialog sound configuration component. The purpose of this component is to give
 * the user the ability to select a sound from a predefined list for specific
 * events that happen.
 */
export const SoundConfiguration = React.memo(SoundConfigurationComponent);

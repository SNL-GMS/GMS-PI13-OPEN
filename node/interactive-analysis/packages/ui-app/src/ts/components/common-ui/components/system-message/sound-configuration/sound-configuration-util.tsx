import { UserProfileTypes } from '@gms/common-graphql';
import { AVAILABLE_SOUND_FILES } from '@gms/common-util';
import { UILogger } from '@gms/ui-apollo';
import { Toaster } from '@gms/ui-util';
import Axios from 'axios';
import includes from 'lodash/includes';
import sortBy from 'lodash/sortBy';
import uniq from 'lodash/uniq';
import { userPreferences } from '~components/common-ui/config/user-preferences';
import { ALL_CATEGORIES, ALL_SEVERITIES, ALL_SUBCATEGORIES, SoundConfigurationRow } from './types';

const BASE_SOUNDS_PATH = userPreferences.baseSoundsPath;

const toaster = new Toaster();

/**
 * Determine which sound files are missing, if any.
 * @returns the missing sound files; empty array if no files are missing
 */
export const getMissingSounds = async (filenames: string[]): Promise<string[]> =>
  Promise.all<string>(
    sortBy<string>(uniq<string>(filenames)).map(async soundFileName =>
      Axios.get(`${BASE_SOUNDS_PATH}${String(soundFileName)}`)
        .then(response => undefined)
        .catch(error => soundFileName)
    )
  ).then(response => response.filter(r => r !== undefined));

/**
 * Validate the available sound files. Toast an error message for
 * each sound file that is not loaded properly (not found).
 */
export const validateAvailableSounds = (): void => {
  getMissingSounds(AVAILABLE_SOUND_FILES)
    .then(missingSoundFiles => {
      missingSoundFiles.forEach(soundFileName => {
        toaster.toastError(userPreferences.soundFileNotFound(soundFileName));
        UILogger.Instance().error(
          userPreferences.soundFileNotFound(`${BASE_SOUNDS_PATH}${soundFileName}`)
        );
      });
    })
    .catch(error => UILogger.Instance().error(error));
};

/**
 * Validate the configured audible notifications against the available sounds.
 * Toast an error message for each sound file that is configured for an audible notifications but not available.
 */
export const validateConfiguredAudibleNotifications = (
  audibleNotifications: UserProfileTypes.AudibleNotification[]
): void => {
  const configuredSoundFiles = sortBy<string>(
    uniq<string>(audibleNotifications?.map(notification => notification.fileName))
  );

  getMissingSounds(configuredSoundFiles)
    .then(missingSoundFiles => {
      const soundFilesInError = configuredSoundFiles.filter(sound =>
        includes(missingSoundFiles, sound)
      );
      soundFilesInError.forEach(soundFileName => {
        toaster.toastError(
          userPreferences.configuredAudibleNotificationFileNotFound(soundFileName)
        );
        UILogger.Instance().error(
          userPreferences.configuredAudibleNotificationFileNotFound(
            `${BASE_SOUNDS_PATH}${soundFileName}`
          )
        );
      });
    })
    .catch(error => UILogger.Instance().error(error));
};

/**
 * @returns the filenames of the sound files that are in the /sounds directory
 * Relative path only. For example /sounds/example.mp3 would be returned as "example.mp3"
 */
export const getAvailableSounds = () => {
  const soundFileNames: string[] = AVAILABLE_SOUND_FILES;
  const obj: { [key: string]: string } = {};
  let soundsMap = soundFileNames.reduce((prev, curr) => {
    prev[curr] = curr;
    return prev;
  }, obj);
  soundsMap = { default: 'None', ...soundsMap };
  return soundsMap;
};

/**
 * Filter rows based on severity, category, and subcategory
 * @param selectedSeverity - selected severity
 * @param rows - The rows to be filtered
 * @param selectedCategory - Selected category
 * @param selectedSubcategory - Selected subcategory
 *
 * @returns - Filtered sound configuration rows
 */
export const filterRowsByType = (
  selectedSeverity: string,
  rows: SoundConfigurationRow[],
  selectedCategory: string,
  selectedSubcategory: string
) => {
  let newRows = [...rows];

  if (selectedSeverity !== ALL_SEVERITIES) {
    newRows = newRows.filter(row => row.severity === selectedSeverity);
  }
  if (selectedCategory !== ALL_CATEGORIES) {
    newRows = newRows.filter(row => row.category === selectedCategory);
  }
  if (selectedSubcategory !== ALL_SUBCATEGORIES) {
    newRows = newRows.filter(row => row.subcategory === selectedSubcategory);
  }
  return newRows;
};

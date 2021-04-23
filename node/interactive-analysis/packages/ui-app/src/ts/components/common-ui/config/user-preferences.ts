export interface UserPreferences {
  baseSoundsPath: string;
  soundFileNotFound(filename: string): string;
  configuredAudibleNotificationFileNotFound(filename: string): string;
}

export const userPreferences: UserPreferences = {
  baseSoundsPath: '/sounds/',
  soundFileNotFound: (filename: string) => `Failed to load sound file "${filename}".`,
  configuredAudibleNotificationFileNotFound: (filename: string) =>
    `Failed to load sound file "${filename}" for configured audible notification. Sound will not play.`
};

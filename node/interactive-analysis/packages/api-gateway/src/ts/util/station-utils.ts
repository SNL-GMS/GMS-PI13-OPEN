import { ProcessingChannel } from '../station/processing-station/model';

/**
 * Converts Processing Channels into OSD compatible Channels
 * @param channels Gateway compatible Processing Channel
 * @returns OSD compatible channel list
 */
export function convertOSDProcessingChannel(channels: ProcessingChannel[]): any[] {
  const osdChannels = [];
  channels.forEach(channel => {
    const newChannel = {
      ...channel
    };
    osdChannels.push(newChannel);
  });
  return osdChannels;
}

import { DataAcquisitionProcessor } from './data-acquisition-processor';
import { TransferredFile } from './model';

// GraphQL Resolvers
export const resolvers = {
  // Query resolvers
  Query: {
    // Retrieve transferred file objects by time range
    transferredFilesByTimeRange: async (_, { timeRange }) =>
      DataAcquisitionProcessor.Instance().getTransferredFilesByTimeRange(timeRange)
  },

  // Mutation Resolvers
  Mutation: {
    saveReferenceStation: async (_, { input }) => {
      const status = await DataAcquisitionProcessor.Instance().saveReferenceStation(input);
      return {
        result: status
      };
    }
  },

  // Field resolvers for FileGap
  FileGap: {
    stationName: async (transferredFile: TransferredFile) => transferredFile.metadata.stationName,
    channelNames: async (transferredFile: TransferredFile) => transferredFile.metadata.channelNames,
    duration: (transferredFile: TransferredFile) =>
      DataAcquisitionProcessor.Instance().processGapDuration(
        transferredFile.metadata.payloadEndTime,
        transferredFile.metadata.payloadStartTime
      ),
    location: (transferredFile: TransferredFile) =>
      DataAcquisitionProcessor.Instance().getLocation(transferredFile),
    startTime: (transferredFile: TransferredFile) => transferredFile.metadata.payloadStartTime,
    endTime: (transferredFile: TransferredFile) => transferredFile.metadata.payloadEndTime,
    priority: (transferredFile: TransferredFile) => transferredFile.priority
  }
};

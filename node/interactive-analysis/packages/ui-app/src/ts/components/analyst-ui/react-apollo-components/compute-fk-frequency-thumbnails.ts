import { FkQueries, FkTypes } from '@gms/common-graphql';
import { Client } from '@gms/ui-apollo';
import { ApolloQueryResult } from 'apollo-client';

/**
 * Create a new detections
 *
 * @param client apollo client
 * @param variables mutation variables
 */
export const computeFkFrequencyThumbnails = async (
  client: Client,
  variables: FkTypes.ComputeFrequencyFkThumbnailsInput
): Promise<ApolloQueryResult<{
  computeFkFrequencyThumbnails?: FkTypes.FkFrequencyThumbnailBySDId;
}>> =>
  client.query<{ computeFkFrequencyThumbnails?: FkTypes.FkFrequencyThumbnailBySDId }>({
    variables,
    query: FkQueries.computeFkFrequencyThumbnailQuery
  });

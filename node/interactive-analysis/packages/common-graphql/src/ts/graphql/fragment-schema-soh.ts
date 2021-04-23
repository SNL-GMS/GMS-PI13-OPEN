/**
 * Use query below to update the schema, by running in playground and copy pasting the result into this file.
 * ! Gateway must be ran in dev mode
 * Use the linter to help resolve copy paste format issues.
 *
 * query {
 *    __schema {
 *       types {
 *         kind
 *         name
 *         possibleTypes {
 *           name
 *         }
 *       }
 *     }
 *   }
 */

export const fragmentSchemaSOH = {
  __schema: {
    types: [
      {
        kind: 'OBJECT',
        name: 'Query',
        possibleTypes: null
      },
      {
        kind: 'SCALAR',
        name: 'Int',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'VersionInfo',
        possibleTypes: null
      },
      {
        kind: 'SCALAR',
        name: 'String',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'UserProfile',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'UserLayout',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'StationAndStationGroupSoh',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'StationGroupSohStatus',
        possibleTypes: null
      },
      {
        kind: 'SCALAR',
        name: 'Float',
        possibleTypes: null
      },
      {
        kind: 'ENUM',
        name: 'SohStatusSummary',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'UiStationSoh',
        possibleTypes: null
      },
      {
        kind: 'SCALAR',
        name: 'Boolean',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'SohContributor',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'UnacknowledgedSohStatusChange',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'SohStatusChange',
        possibleTypes: null
      },
      {
        kind: 'ENUM',
        name: 'SohMonitorType',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'Mutation',
        possibleTypes: null
      },
      {
        kind: 'INPUT_OBJECT',
        name: 'ClientLogInput',
        possibleTypes: null
      },
      {
        kind: 'ENUM',
        name: 'LogLevel',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'ClientLog',
        possibleTypes: null
      },
      {
        kind: 'INPUT_OBJECT',
        name: 'UserLayoutInput',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'Subscription',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: '__Schema',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: '__Type',
        possibleTypes: null
      },
      {
        kind: 'ENUM',
        name: '__TypeKind',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: '__Field',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: '__InputValue',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: '__EnumValue',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: '__Directive',
        possibleTypes: null
      },
      {
        kind: 'ENUM',
        name: '__DirectiveLocation',
        possibleTypes: null
      },
      {
        kind: 'SCALAR',
        name: 'Date',
        possibleTypes: null
      },
      {
        kind: 'INPUT_OBJECT',
        name: 'TimeRange',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'StationSohIssue',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'SohStatus',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'StationAcquisitionSohStatus',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'EnvironmentSohStatus',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'CountBySoh',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'SummaryBySoh',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'ChannelSohStatus',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'SaveStationGroupSohStatusResult',
        possibleTypes: null
      },
      {
        kind: 'ENUM',
        name: 'SohStatusSummaryInput',
        possibleTypes: null
      },
      {
        kind: 'INPUT_OBJECT',
        name: 'UiStationSohInput',
        possibleTypes: null
      },
      {
        kind: 'INPUT_OBJECT',
        name: 'StationSohIssueInput',
        possibleTypes: null
      },
      {
        kind: 'INPUT_OBJECT',
        name: 'SohStatusInput',
        possibleTypes: null
      },
      {
        kind: 'INPUT_OBJECT',
        name: 'StationAcquisitionSohStatusInput',
        possibleTypes: null
      },
      {
        kind: 'INPUT_OBJECT',
        name: 'EnvironmentSohStatusInput',
        possibleTypes: null
      },
      {
        kind: 'INPUT_OBJECT',
        name: 'CountBySohInput',
        possibleTypes: null
      },
      {
        kind: 'INPUT_OBJECT',
        name: 'SummaryBySohInput',
        possibleTypes: null
      },
      {
        kind: 'INPUT_OBJECT',
        name: 'ChannelSohStatusInput',
        possibleTypes: null
      }
    ]
  }
};

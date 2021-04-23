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

export const fragmentSchema = {
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
        name: 'WorkspaceState',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'EventToUsers',
        possibleTypes: null
      },
      {
        kind: 'SCALAR',
        name: 'String',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'VersionInfo',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'Analyst',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'ProcessingStage',
        possibleTypes: null
      },
      {
        kind: 'ENUM',
        name: 'ProcessingStageType',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'ProcessingActivity',
        possibleTypes: null
      },
      {
        kind: 'ENUM',
        name: 'ProcessingActivityType',
        possibleTypes: null
      },
      {
        kind: 'INPUT_OBJECT',
        name: 'TimeRange',
        possibleTypes: null
      },
      {
        kind: 'SCALAR',
        name: 'Float',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'ProcessingStageInterval',
        possibleTypes: null
      },
      {
        kind: 'ENUM',
        name: 'IntervalStatus',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'ProcessingActivityInterval',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'ProcessingInterval',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'ProcessingStation',
        possibleTypes: null
      },
      {
        kind: 'ENUM',
        name: 'StationType',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'Location',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'ProcessingChannelGroup',
        possibleTypes: null
      },
      {
        kind: 'ENUM',
        name: 'ChannelGroupType',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'ProcessingChannel',
        possibleTypes: null
      },
      {
        kind: 'ENUM',
        name: 'ChannelDataType',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'Orientation',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'ReferenceStation',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'ReferenceSite',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'ReferenceChannel',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'Position',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'ReferenceNetwork',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'DataAcquisition',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'ChannelSegment',
        possibleTypes: null
      },
      {
        kind: 'ENUM',
        name: 'TimeSeriesType',
        possibleTypes: null
      },
      {
        kind: 'ENUM',
        name: 'ChannelSegmentType',
        possibleTypes: null
      },
      {
        kind: 'INTERFACE',
        name: 'Timeseries',
        possibleTypes: [
          {
            name: 'FkPowerSpectra'
          },
          {
            name: 'Waveform'
          }
        ]
      },
      {
        kind: 'OBJECT',
        name: 'FilteredChannelSegment',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'SignalDetection',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'SignalDetectionHypothesis',
        possibleTypes: null
      },
      {
        kind: 'SCALAR',
        name: 'Boolean',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'FeatureMeasurement',
        possibleTypes: null
      },
      {
        kind: 'ENUM',
        name: 'FeatureMeasurementTypeName',
        possibleTypes: null
      },
      {
        kind: 'UNION',
        name: 'FeatureMeasurementValue',
        possibleTypes: [
          {
            name: 'AmplitudeMeasurementValue'
          },
          {
            name: 'InstantMeasurementValue'
          },
          {
            name: 'NumericMeasurementValue'
          },
          {
            name: 'PhaseTypeMeasurementValue'
          },
          {
            name: 'StringMeasurementValue'
          }
        ]
      },
      {
        kind: 'OBJECT',
        name: 'AmplitudeMeasurementValue',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'DoubleValue',
        possibleTypes: null
      },
      {
        kind: 'ENUM',
        name: 'Units',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'InstantMeasurementValue',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'NumericMeasurementValue',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'PhaseTypeMeasurementValue',
        possibleTypes: null
      },
      {
        kind: 'ENUM',
        name: 'PhaseType',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'StringMeasurementValue',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'SignalDetectionHypothesisHistory',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'ConflictingSdHypData',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'Reviewed',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'RequiresReview',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'EventHypothesis',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'Event',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'PreferredEventHypothesis',
        possibleTypes: null
      },
      {
        kind: 'ENUM',
        name: 'EventStatus',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'DistanceToSource',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'Distance',
        possibleTypes: null
      },
      {
        kind: 'ENUM',
        name: 'DistanceSourceType',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'SignalDetectionEventAssociation',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'LocationSolutionSet',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'LocationSolution',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'EventLocation',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'FeaturePrediction',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'FeaturePredictionComponent',
        possibleTypes: null
      },
      {
        kind: 'ENUM',
        name: 'FeaturePredictionCorrectionType',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'LocationRestraint',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'LocationUncertainty',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'Ellipse',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'Ellipsoid',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'LocationBehavior',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'NetworkMagnitudeSolution',
        possibleTypes: null
      },
      {
        kind: 'ENUM',
        name: 'MagnitudeType',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'NetworkMagnitudeBehavior',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'StationMagnitudeSolution',
        possibleTypes: null
      },
      {
        kind: 'ENUM',
        name: 'MagnitudeModel',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'LocationToStationDistance',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'SignalDetectionSnapshot',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'EventSignalDetectionAssociationValues',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'AmplitudeSnapshot',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'PreferredLocationSolution',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'QcMask',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'QcMaskVersion',
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
        name: 'WaveformFilter',
        possibleTypes: null
      },
      {
        kind: 'INPUT_OBJECT',
        name: 'FkInput',
        possibleTypes: null
      },
      {
        kind: 'INPUT_OBJECT',
        name: 'FrequencyBandInput',
        possibleTypes: null
      },
      {
        kind: 'INPUT_OBJECT',
        name: 'WindowParametersInput',
        possibleTypes: null
      },
      {
        kind: 'INPUT_OBJECT',
        name: 'FkConfigurationInput',
        possibleTypes: null
      },
      {
        kind: 'INPUT_OBJECT',
        name: 'ContributingChannelsConfigurationInput',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'FkFrequencyThumbnailBySDId',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'FkFrequencyThumbnail',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'FrequencyBand',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'FkPowerSpectra',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'FkMetaData',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'FkPowerSpectrum',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'FkAttributes',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'FstatData',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'Waveform',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'FkConfiguration',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'ContributingChannelsConfiguration',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'FileGap',
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
        name: 'History',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'HistoryChange',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'HypothesisChangeInformation',
        possibleTypes: null
      },
      {
        kind: 'ENUM',
        name: 'HypothesisType',
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
        name: 'IntervalStatusInput',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'MarkActivityIntervalResult',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'DataPayload',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'InvalidData',
        possibleTypes: null
      },
      {
        kind: 'INPUT_OBJECT',
        name: 'NewDetectionInput',
        possibleTypes: null
      },
      {
        kind: 'INPUT_OBJECT',
        name: 'SignalDetectionTimingInput',
        possibleTypes: null
      },
      {
        kind: 'INPUT_OBJECT',
        name: 'AmplitudeMeasurementValueInput',
        possibleTypes: null
      },
      {
        kind: 'INPUT_OBJECT',
        name: 'DoubleValueInput',
        possibleTypes: null
      },
      {
        kind: 'INPUT_OBJECT',
        name: 'UpdateDetectionInput',
        possibleTypes: null
      },
      {
        kind: 'INPUT_OBJECT',
        name: 'UpdateEventInput',
        possibleTypes: null
      },
      {
        kind: 'INPUT_OBJECT',
        name: 'UpdateEventHypothesisInput',
        possibleTypes: null
      },
      {
        kind: 'INPUT_OBJECT',
        name: 'EventLocationInput',
        possibleTypes: null
      },
      {
        kind: 'INPUT_OBJECT',
        name: 'LocationBehaviorInput',
        possibleTypes: null
      },
      {
        kind: 'INPUT_OBJECT',
        name: 'ComputeNetworkMagnitudeSolutionInput',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'ComputeNetworkMagnitudeDataPayload',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'RejectedMagnitudeInput',
        possibleTypes: null
      },
      {
        kind: 'INPUT_OBJECT',
        name: 'QcMaskInput',
        possibleTypes: null
      },
      {
        kind: 'INPUT_OBJECT',
        name: 'UserLayoutInput',
        possibleTypes: null
      },
      {
        kind: 'INPUT_OBJECT',
        name: 'FkLeadInput',
        possibleTypes: null
      },
      {
        kind: 'INPUT_OBJECT',
        name: 'MarkFksReviewedInput',
        possibleTypes: null
      },
      {
        kind: 'INPUT_OBJECT',
        name: 'DataAcqReferenceStation',
        possibleTypes: null
      },
      {
        kind: 'INPUT_OBJECT',
        name: 'InformationSourceInput',
        possibleTypes: null
      },
      {
        kind: 'INPUT_OBJECT',
        name: 'ReferenceAliasInput',
        possibleTypes: null
      },
      {
        kind: 'ENUM',
        name: 'StatusTypeName',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'SaveStationResult',
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
        name: 'LocationInput',
        possibleTypes: null
      },
      {
        kind: 'ENUM',
        name: 'DistanceUnits',
        possibleTypes: null
      },
      {
        kind: 'INPUT_OBJECT',
        name: 'DistanceToSourceInput',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'InformationSource',
        possibleTypes: null
      },
      {
        kind: 'ENUM',
        name: 'ConfigurationInfoStatus',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'ConfigurationInfo',
        possibleTypes: null
      },
      {
        kind: 'INPUT_OBJECT',
        name: 'ProcessingContextInput',
        possibleTypes: null
      },
      {
        kind: 'INPUT_OBJECT',
        name: 'AnalystActionReferenceInput',
        possibleTypes: null
      },
      {
        kind: 'INPUT_OBJECT',
        name: 'ProcessingStepReferenceInput',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'Alias',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'ProcessingCalibration',
        possibleTypes: null
      },
      {
        kind: 'INPUT_OBJECT',
        name: 'ProcessingCalibrationInput',
        possibleTypes: null
      },
      {
        kind: 'INPUT_OBJECT',
        name: 'TimeseriesInput',
        possibleTypes: null
      },
      {
        kind: 'INPUT_OBJECT',
        name: 'WaveformInput',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'ProcessingStationGroup',
        possibleTypes: null
      },
      {
        kind: 'INPUT_OBJECT',
        name: 'ForStage',
        possibleTypes: null
      },
      {
        kind: 'OBJECT',
        name: 'SignalDetectionEventAssociationInput',
        possibleTypes: null
      },
      {
        kind: 'INPUT_OBJECT',
        name: 'CreateEventHypothesisInput',
        possibleTypes: null
      },
      {
        kind: 'INPUT_OBJECT',
        name: 'WaveformFilterInput',
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
  // tslint:disable-next-line: max-file-line-count
};

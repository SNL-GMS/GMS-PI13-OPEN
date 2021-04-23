import {
  SystemMessageCategory,
  SystemMessageSeverity,
  SystemMessageSubCategory,
  SystemMessageType
} from '../../../../../common-graphql/src/ts/graphql/system-message/types';

export const systemMessages = [
  {
    id: 'mock1',
    category: SystemMessageCategory.SOH,
    message: 'mock message text',
    severity: SystemMessageSeverity.CRITICAL,
    subCategory: SystemMessageSubCategory.STATION,
    time: 1593030714074,
    type: SystemMessageType.STATION_NEEDS_ATTENTION
  },
  {
    id: 'mock2',
    category: SystemMessageCategory.SOH,
    message: 'mock message text',
    severity: SystemMessageSeverity.CRITICAL,
    subCategory: SystemMessageSubCategory.STATION,
    time: 1593030714075,
    type: SystemMessageType.STATION_NEEDS_ATTENTION
  }
];

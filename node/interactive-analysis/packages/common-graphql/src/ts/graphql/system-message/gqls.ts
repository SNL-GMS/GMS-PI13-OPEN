import gql from 'graphql-tag';

/**
 * Defines the graphql fragment for a system message.
 */
export const systemMessageFragment = gql`
  fragment SystemMessageFragment on SystemMessage {
    id
    time
    message
    type
    severity
    category
    subCategory
  }
`;

/**
 * Defines the graphql fragment for a system message definition.
 */
export const systemMessageDefinitionFragment = gql`
  fragment SystemMessageDefinitionFragment on SystemMessageDefinition {
    systemMessageType
    systemMessageCategory
    systemMessageSubCategory
    systemMessageSeverity
    template
  }
`;

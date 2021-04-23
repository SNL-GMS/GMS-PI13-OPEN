import React from 'react';
import { InteractionConsumer } from './interaction-consumer';
import { InteractionProvider } from './interaction-provider';

/**
 * Wrap the component with interaction handling
 */
export const InteractionWrapper = (Component: React.ComponentClass): React.ComponentClass =>
  class extends React.Component<any, any> {
    /**
     * Wrap the component in an apollo and redux providers
     */
    public render() {
      return (
        <InteractionProvider>
          <InteractionConsumer>
            <Component {...this.props} />
          </InteractionConsumer>
        </InteractionProvider>
      );
    }
  };

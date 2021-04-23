import { Classes, Intent, NonIdealState, Spinner } from '@blueprintjs/core';
import { IconNames } from '@blueprintjs/icons';
import { getKeyPressAction } from '@gms/ui-state';
import React from 'react';
import { GoldenLayout } from './components/golden-layout';
import { KeyContext, WorkspaceProps, WorkspaceState } from './types';

// tslint:disable-next-line: no-require-imports no-var-requires
const logo = require('../../resources/gms-logo.png');

/**
 * Primary analyst workspace component. Uses golden-layout to create a configurable display of multiple
 * sub-components.
 */
export class Workspace extends React.Component<WorkspaceProps, WorkspaceState> {
  public constructor(props) {
    super(props);
    this.state = {
      triggerAnimation: false
    };
  }

  /**
   * Create the analyst workspace
   */
  public render() {
    return (
      <div
        className={`${Classes.DARK} workspace-container`}
        tabIndex={0}
        onKeyDown={this.handleWorkspaceHotkey}
        data-cy="workspace"
      >
        {this.props.userSessionState.connected ? (
          <KeyContext.Provider
            value={{
              shouldTriggerAnimation: this.state.triggerAnimation,
              resetAnimation: () => {
                this.setState({ triggerAnimation: false });
              }
            }}
          >
            <GoldenLayout
              logo={logo}
              userName={this.props.userSessionState.authorizationStatus.userName}
            />
          </KeyContext.Provider>
        ) : (
          <div className={`${Classes.DARK} workspace-invalid-state`}>
            <div className="gms-disconnected">
              <NonIdealState
                icon={IconNames.ERROR}
                action={<Spinner intent={Intent.DANGER} />}
                title="No connection to server"
                description="Attempting to connect..."
              />
            </div>
          </div>
        )}
      </div>
    );
  }

  private readonly handleWorkspaceHotkey = (
    keyEvent: React.KeyboardEvent<HTMLDivElement>
  ): void => {
    if (!keyEvent.repeat) {
      const keyPressAction = getKeyPressAction(keyEvent);
      if (keyPressAction) {
        keyEvent.stopPropagation();
        keyEvent.preventDefault();
        const entryForKeyMap = this.props.keyPressActionQueue.get(keyPressAction)
          ? this.props.keyPressActionQueue.get(keyPressAction)
          : 0;
        const updatedMap = this.props.keyPressActionQueue.set(keyPressAction, entryForKeyMap + 1);
        this.setState(
          {
            triggerAnimation: true
          },
          () => {
            this.props.setKeyPressActionQueue(updatedMap);
            this.setState({ triggerAnimation: false });
          }
        );
      }
    }
  }
}

import { Intent, NonIdealState, Spinner } from '@blueprintjs/core';
import { reload } from '@gms/ui-util';
import React from 'react';
import { unAuthenticateWith } from '../../../../authentication';
import { GoldenLayoutPanel } from './golden-layout-panel';
import { GoldenLayoutComponentProps } from './types';

export class GoldenLayoutComponent extends React.Component<GoldenLayoutComponentProps, {}> {
  public constructor(props) {
    super(props);
  }

  public render() {
    const maybeUserProfile =
      this.props.userProfileQuery && this.props.userProfileQuery.userProfile
        ? this.props.userProfileQuery.userProfile
        : undefined;
    const maybeVersionInfo =
      this.props.versionInfoQuery && this.props.versionInfoQuery.versionInfo
        ? this.props.versionInfoQuery.versionInfo
        : undefined;

    return maybeUserProfile && maybeVersionInfo ? (
      <GoldenLayoutPanel
        logo={this.props.logo}
        openLayoutName={this.props.openLayoutName}
        setOpenLayoutName={this.props.setOpenLayoutName}
        userName={this.props.userName}
        userProfile={maybeUserProfile}
        versionInfo={maybeVersionInfo}
        setLayout={this.props.setLayout}
        logout={() => {
          this.logout();
        }}
      />
    ) : (
      <NonIdealState
        action={<Spinner intent={Intent.PRIMARY} />}
        title="Loading Default Layout"
        description="Retrieving default layout for user..."
      />
    );
  }

  /**
   * Logs the user out of the system
   */
  private readonly logout = () => {
    unAuthenticateWith()
      .then(result => {
        this.props.setAuthStatus(result);
      })
      // tslint:disable-next-line: no-console
      .catch(error => console.error(`Failed to un-authenticate: ${error}`));
    // TODO figure out how to avoid the need to reload the page
    reload();
  }
}

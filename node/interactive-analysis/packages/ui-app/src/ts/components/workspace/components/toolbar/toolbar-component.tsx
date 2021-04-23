import { Button, Classes, Dialog, Popover } from '@blueprintjs/core';
import { IconNames } from '@blueprintjs/icons';
import { UILogger } from '@gms/ui-apollo';
import React from 'react';
import { KeyContext } from '../../types';
import { AboutMenu, AppMenu } from '../menus';
import { ToolbarProps } from './types';

/**
 * * triggerAnimation
 * toggles the animation class on the app logo so that it flashes.
 * @param AnimationTarget the reference object from the useRef hook that tracks the animation target
 */
export const triggerAnimation = (AnimationTarget: React.MutableRefObject<HTMLSpanElement>) => {
  if (AnimationTarget.current) {
    AnimationTarget.current.className = 'workspace-logo__label';
    // Placing a call to get to object's current offset width between adding/removing classes
    // forces the browser to render the "animation-less" version of the component
    // This is the current recommended solution on stack overflow
    // This shouldn't impact performance - as css animations already cause reflows
    const assignmentToForceReflow = AnimationTarget.current.offsetWidth;
    UILogger.Instance().info(
      `${assignmentToForceReflow} required output to ignore sonar lint rule`
    );
    AnimationTarget.current.className = 'workspace-logo__label keypress-signifier';
  }
};

/**
 * * Toolbar
 * Build the Toolbar, including logo and app-level menu
 */
export const Toolbar: React.FunctionComponent<ToolbarProps> = props => {
  // The width and height of the logo
  const logoSize = {
    width: 35,
    height: 33
  };
  const AnimationTarget = React.useRef<HTMLSpanElement>(undefined);

  const keyContext = React.useContext(KeyContext);
  const { shouldTriggerAnimation } = keyContext;
  React.useEffect(() => triggerAnimation(AnimationTarget), [shouldTriggerAnimation]);

  const {
    clearLayout,
    getOpenDisplays,
    logout,
    openDisplay,
    openWorkspace,
    showAboutDialog,
    showLogPopup,
    toggleSaveWorkspaceAsDialog
  } = props;

  return (
    <nav className={`${Classes.NAVBAR} workspace-navbar`}>
      <div className="workspace-navbar__image-label">
        <img
          src={props.logo}
          alt="GMS Logo"
          height={logoSize.height}
          width={logoSize.width}
          className="workspace-logo"
        />
        <span
          className={`workspace-logo__label`}
          ref={AnimationTarget}
          onAnimationEnd={e => (e.currentTarget.className = 'workspace-logo__label')}
        >
          GMS
        </span>
        {props.saveDialog}
      </div>
      <Popover
        content={
          <AppMenu
            components={props.components}
            clearLayout={clearLayout}
            openLayoutName={props.openLayoutName}
            getOpenDisplays={getOpenDisplays}
            logo={props.logo}
            logout={logout}
            openDisplay={openDisplay}
            openWorkspace={openWorkspace}
            saveWorkspaceAs={toggleSaveWorkspaceAsDialog}
            showAboutDialog={showAboutDialog}
            showLogs={showLogPopup}
            userProfile={props.userProfile}
          />
        }
      >
        <Button
          data-cy="username"
          className="app-menu-button"
          rightIcon={IconNames.USER}
          text={props.userName}
          minimal={true}
        />
      </Popover>
      <Dialog
        isOpen={props.isAboutDialogOpen}
        onClose={showAboutDialog}
        className={Classes.DARK}
        title={'About'}
      >
        <AboutMenu
          commitSHA={props.versionInfo.commitSHA}
          versionNumber={props.versionInfo.versionNumber}
          logo={props.logo}
        />
      </Dialog>
    </nav>
  );
};

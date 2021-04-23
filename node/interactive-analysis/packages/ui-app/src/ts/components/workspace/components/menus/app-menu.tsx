import { Menu, MenuDivider, MenuItem } from '@blueprintjs/core';
import { IconNames } from '@blueprintjs/icons';
import { UserProfileTypes } from '@gms/common-graphql';
import { KeyValue } from '@gms/common-util';
import { uniqBy } from 'lodash';
import React from 'react';
import {
  GLComponentConfig,
  GLComponentMap,
  GLComponentValue,
  GLMap,
  GoldenLayoutContext,
  isGLComponentMap,
  isGLKeyValue
} from '../golden-layout/types';

/**
 * Generates an array of sub menu items
 * @param components map of components to create sub menu items for
 * @param openDisplay method that should be called after an onClick
 */
export const generateAppMenuDisplayOptions = (
  components: GLMap | GLComponentMap,
  openDisplay: (componentKey: string) => void,
  getOpenDisplays: () => string[]
): JSX.Element[] => {
  const getValue = (key: string, val: GLComponentValue): string =>
    isGLComponentMap(val) ? key.toLowerCase() : isGLKeyValue(val) ? val.id.title.toLowerCase() : '';

  const sort = (values: GLMap) =>
    [...values].sort(([keyA, componentA], [keyB, componentB]) => {
      const componentAVal = getValue(keyA, componentA);
      const componentBVal = getValue(keyB, componentB);
      return componentAVal.localeCompare(componentBVal);
    });

  return sort(components).map(([key, component]) => {
    if (isGLComponentMap(component)) {
      return (
        <MenuItem text={`${key}`} key={key} icon={IconNames.DESKTOP}>
          {...generateAppMenuDisplayOptions(component, openDisplay, getOpenDisplays)}
        </MenuItem>
      );
    }

    if (isGLKeyValue(component)) {
      return (
        <MenuItem
          text={component.id.title}
          key={key}
          onClick={() => {
            openDisplay(key);
          }}
          disabled={
            getOpenDisplays().find(display => display === component.id.component) !== undefined
          }
        />
      );
    }
    return undefined;
  });
};

export interface AppMenuProps {
  components: Map<
    string,
    | KeyValue<GLComponentConfig, React.ComponentClass>
    | Map<string, KeyValue<GLComponentConfig, React.ComponentClass>>
  >;
  logo: any;
  userProfile: UserProfileTypes.UserProfile;
  openLayoutName: string;
  getOpenDisplays(): string[];
  logout(): void;
  showLogs(): void;
  clearLayout(): void;
  openDisplay(componentKey: string): void;
  openWorkspace(layout: UserProfileTypes.UserLayout): void;
  showAboutDialog(): void;
  saveWorkspaceAs(): void;
}

/**
 * Create the app menu component for use in the top-level app menu popover
 */
export const AppMenu: React.FunctionComponent<AppMenuProps> = (props: AppMenuProps) => {
  const context = React.useContext(GoldenLayoutContext);
  const defaultLayout = props.userProfile.workspaceLayouts.find(
    wl => wl.name === props.userProfile.defaultLayoutName
  );
  // Get a list of layouts that are uniq by name - this is a limitation due to the current
  // way layouts are stored in the database. It causes duplicates in the UI.
  const layouts = uniqBy(
    props.userProfile.workspaceLayouts
      .filter(wl => wl.name !== props.userProfile.defaultLayoutName)
      .filter(layout =>
        layout.supportedUserInterfaceModes.includes(context.supportedUserInterfaceMode)
      ),
    wl => wl.name
  );
  layouts.sort((a, b) => a.name.localeCompare(b.name));
  return (
    <Menu>
      <MenuItem
        className="app-menu__about"
        onClick={(event: React.MouseEvent) => {
          props.showAboutDialog();
        }}
        text="About"
        title="Shows GMS system version info"
        icon={IconNames.INFO_SIGN}
      />
      <MenuDivider key="Workspace" title="Workspace" className={'menu-title'} />
      {props.userProfile !== undefined ? (
        <MenuItem
          text="Open Workspace"
          title="Opens a previously saved workspace"
          className="app-menu__open-workspace"
          icon={IconNames.FOLDER_OPEN}
        >
          <MenuItem
            className={defaultLayout?.name !== props.openLayoutName ? 'unopened-layout' : ''}
            icon={defaultLayout?.name === props.openLayoutName ? IconNames.TICK : null}
            text={`${defaultLayout?.name} (default)`}
            onClick={() => props.openWorkspace(defaultLayout)}
          />
          {layouts.length > 0 ? (
            <React.Fragment>
              <MenuDivider />
              {layouts.map((wl, index) => (
                <MenuItem
                  key={index}
                  className={wl.name !== props.openLayoutName ? 'unopened-layout' : ''}
                  icon={wl.name === props.openLayoutName ? IconNames.TICK : null}
                  text={`${wl.name}`}
                  onClick={() => props.openWorkspace(wl)}
                />
              ))}
            </React.Fragment>
          ) : null}
        </MenuItem>
      ) : null}
      <MenuItem
        onClick={(event: React.MouseEvent) => {
          props.saveWorkspaceAs();
        }}
        text="Save Workspace As"
        className="app-menu__save-as"
        title="Save your current workspace layout as a new layout"
        icon={IconNames.FLOPPY_DISK}
      />
      <MenuDivider key="Displays" title="Displays" className={'menu-title'} />
      {...generateAppMenuDisplayOptions(
        props.components,
        key => {
          props.openDisplay(key);
        },
        () => props.getOpenDisplays()
      )}
      <MenuDivider />
      <MenuItem
        text="Developer Tools"
        title="Shows advanced debugging tools"
        icon={IconNames.WRENCH}
      >
        <MenuItem
          onClick={() => {
            props.showLogs();
          }}
          className="app-menu__logs"
          text="Logs"
          title="Shows logs between UI and API Gateway"
        />
        <MenuItem
          onClick={() => {
            props.clearLayout();
          }}
          text="Clear Layout"
          className="app-menu__clear-layout"
          title="Clears local golden layout configuration and replaces with pre-configured default"
        />
      </MenuItem>
      <MenuDivider />
      <MenuItem
        onClick={e => {
          props.logout();
        }}
        text="Log out"
        className="app-menu__logout"
        icon={IconNames.LOG_OUT}
      />
    </Menu>
  );
};

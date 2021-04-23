import { CommonTypes, UserProfileTypes } from '@gms/common-graphql';
import { KeyValue, UserMode } from '@gms/common-util';
import GoldenLayout from '@gms/golden-layout';
import { AnalystWorkspaceTypes, UserSessionTypes } from '@gms/ui-state';
import Immutable from 'immutable';
import React from 'react';
import { MutationFunction } from 'react-apollo';

export interface GoldenLayoutPanelProps {
  logo: any;
  userName: string;
  openLayoutName: string;
  versionInfo: CommonTypes.VersionInfo;
  userProfile: UserProfileTypes.UserProfile;
  setLayout: MutationFunction<{}>;
  setOpenLayoutName(name: string);
  logout(): void;
}

export interface GoldenLayoutPanelState {
  selectedWorkspaceId: string;
  saveAsName: string;
  isSaveAsDefaultChecked: boolean;
  isSaveWorkspaceAsDialogOpen: boolean;
  isSaveWorkspaceOnChangeDialogOpen: boolean;
  isAboutDialogOpen: boolean;
  isSaveLayoutChangesOpen: boolean;
  userLayoutToOpen: UserProfileTypes.UserLayout;
}

export interface GoldenLayoutComponentBaseProps {
  logo: any;
  userName: string;
  currentTimeInterval: CommonTypes.TimeRange;
  analystActivity: AnalystWorkspaceTypes.AnalystActivity;
  setAuthStatus(auth: UserSessionTypes.AuthStatus): void;
}

export interface GoldenLayoutComponentReduxProps {
  openLayoutName: string;
  keyPressActionQueue: Immutable.Map<AnalystWorkspaceTypes.KeyAction, number>;
  setKeyPressActionQueue(actions: Immutable.Map<AnalystWorkspaceTypes.KeyAction, number>): void;
  setOpenLayoutName(name: string);
}

/**
 * Mutations used in the event list
 */
export interface GoldenLayoutComponentMutations {
  setLayout: MutationFunction<{}>;
}

export type GoldenLayoutComponentProps = CommonTypes.VersionInfoProps &
  GoldenLayoutComponentBaseProps &
  GoldenLayoutComponentReduxProps &
  UserProfileTypes.UserProfileProps &
  GoldenLayoutComponentMutations;

/** Defines the GL component config */
export interface GLComponentConfig {
  type: string;
  title: string;
  component: string;
}

/** Defines the GL component config list */
export interface GLComponentConfigList {
  [componentKey: string]: GLComponentConfig;
}

/** Defines the GL config */
export interface GLConfig {
  components: GLComponentConfigList;
  workspace: GoldenLayout.Config;
}

/** Defines the GL key value */
export type GLKeyValue = KeyValue<GLComponentConfig, React.ComponentClass>;

/** Defines the GL component map */
export type GLComponentMap = Map<string, GLKeyValue>;

/** Defines the GL component value */
export type GLComponentValue = GLKeyValue | GLComponentMap;

/** Defines the GL Map */
export type GLMap = Map<string, GLComponentValue>;

/**
 * Returns true if the value is a GLComponentMap
 * @param val the value to check
 */
export const isGLComponentMap = (val: GLComponentValue): val is GLComponentMap =>
  val instanceof Map;

/**
 * Returns true if the value is a GLKeyValue
 * @param val the value to check
 */
export const isGLKeyValue = (val: GLComponentValue): val is GLKeyValue => !isGLComponentMap(val);

/** The Golden Layout context data */
export interface GoldenLayoutContextData {
  config: GLConfig;
  glComponents: GLMap;
  supportedUserInterfaceMode: UserMode;
}

/** The Golden Layout context */
export const GoldenLayoutContext = React.createContext<GoldenLayoutContextData>({
  config: undefined,
  glComponents: undefined,
  supportedUserInterfaceMode: undefined
});

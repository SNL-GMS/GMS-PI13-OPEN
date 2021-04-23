import { CommonTypes, UserProfileTypes } from '@gms/common-graphql';
import { MutationFunction } from 'react-apollo';
import { GLMap } from '../golden-layout/types';

export interface ToolbarBaseProps {
  components: GLMap;
  logo: any;
  userName: string;
  isAboutDialogOpen: boolean;
  isSaveWorkspaceAsDialogOpen: boolean;
  openLayoutName: string;
  versionInfo: CommonTypes.VersionInfo;
  userProfile?: UserProfileTypes.UserProfile;
  setLayout: MutationFunction<{}>;
  saveDialog: JSX.Element;

  getOpenDisplays(): string[];
  clearLayout(): void;
  logout(): void;
  openDisplay(displayKey: string): void;
  openWorkspace(layout: UserProfileTypes.UserLayout): void;
  toggleSaveWorkspaceAsDialog(): void;
  showLogPopup(): void;
  showAboutDialog(): void;
  setOpenLayoutName(name: string);
}

export type ToolbarProps = ToolbarBaseProps;

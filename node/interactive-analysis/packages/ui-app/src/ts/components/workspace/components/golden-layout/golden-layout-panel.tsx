import { Checkbox } from '@blueprintjs/core';
import { UserProfileTypes } from '@gms/common-graphql';
import { IS_INTERACTIVE_ANALYSIS_MODE_SOH, SUPPORTED_MODES } from '@gms/common-util';
import GoldenLayout from '@gms/golden-layout';
import { UILogger } from '@gms/ui-apollo';
import { ModalPrompt, SaveOpenDialog } from '@gms/ui-core-components';
import { getElectron, isElectron, Toaster } from '@gms/ui-util';
import elementResizeEvent from 'element-resize-event';
import { uniqBy } from 'lodash';
import debounce from 'lodash/debounce';
import flatMap from 'lodash/flatMap';
import React from 'react';
import { Toolbar } from '../toolbar';
import {
  GLComponentValue,
  GLKeyValue,
  GoldenLayoutContext,
  GoldenLayoutContextData,
  GoldenLayoutPanelProps,
  GoldenLayoutPanelState,
  isGLComponentMap,
  isGLKeyValue
} from './types';

// Electron instance; undefined if not running in electron
const electron = getElectron();

export class GoldenLayoutPanel extends React.Component<
  GoldenLayoutPanelProps,
  GoldenLayoutPanelState
> {
  /** The toaster reference for user notification pop-ups */
  private static readonly toaster: Toaster = new Toaster();

  /** The Golden Layout context - provides the layout configuration */
  public static contextType: React.Context<GoldenLayoutContextData> = GoldenLayoutContext;

  /** The Golden Layout context - provides the layout configuration */
  public context: React.ContextType<typeof GoldenLayoutContext>;

  /**
   * Handle to the dom element where we will render the golden-layout workspace
   */
  private glContainerRef: HTMLDivElement;

  private gl: GoldenLayout;

  public constructor(props: GoldenLayoutPanelProps) {
    super(props);
    this.state = {
      isAboutDialogOpen: false,
      isSaveWorkspaceAsDialogOpen: false,
      isSaveLayoutChangesOpen: false,
      isSaveWorkspaceOnChangeDialogOpen: false,
      isSaveAsDefaultChecked: false,
      saveAsName: '',
      selectedWorkspaceId: this.props.openLayoutName,
      userLayoutToOpen: undefined
    };
  }

  public componentDidUpdate(prevProps: GoldenLayoutPanelProps) {
    if (prevProps.openLayoutName !== this.props.openLayoutName) {
      this.setState({
        saveAsName: this.props.openLayoutName,
        selectedWorkspaceId: this.props.openLayoutName
      });
    }
  }

  public render() {
    const defaultLayoutName = this.props.userProfile.defaultLayoutName;
    const saveDialog = (
      <SaveOpenDialog
        title="Save Workspace As"
        actionText="Save"
        actionTooltipText="Save this workspace layout"
        // Get a list of layouts that are uniq by name - this is a limitation due to the current
        // way layouts are stored in the database. It causes duplicates in the UI.
        itemList={uniqBy(
          this.props.userProfile.workspaceLayouts.filter(layout =>
            layout.supportedUserInterfaceModes.includes(this.context.supportedUserInterfaceMode)
          ),
          wl => wl.name
        )
          .map(l => ({ id: l.name, title: l.name }))
          .sort((a, b) => a.title.localeCompare(b.title))}
        selectedId={this.state.selectedWorkspaceId}
        defaultId={defaultLayoutName}
        openedItemId={this.props.openLayoutName}
        titleOfItemList="Existing Workspace: "
        actionCallback={async () => {
          await this.handleAffirmativeAction();
        }}
        cancelCallback={() => {
          this.toggleSaveWorkspaceAsDialog();
        }}
        selectEntryCallback={(id: string) => {
          this.setState({
            selectedWorkspaceId: id,
            saveAsName: id
          });
        }}
        isDialogOpen={this.state.isSaveWorkspaceAsDialogOpen}
      >
        <label>
          Name:
          <input
            name="save-name"
            onChange={(event: React.ChangeEvent<HTMLInputElement>) => {
              this.setState({
                saveAsName: event.currentTarget.value,
                selectedWorkspaceId: undefined
              });
            }}
            onKeyPress={async (event: React.KeyboardEvent<HTMLInputElement>) => {
              switch (event.key) {
                case 'Enter':
                  await this.handleAffirmativeAction();
                  break;
                case 'Escape':
                  event.stopPropagation();
                  break;
                default:
              }
            }}
            placeholder="Enter name..."
            value={this.state.saveAsName}
          />
        </label>
        <Checkbox
          label="Save as default"
          checked={this.state.isSaveAsDefaultChecked}
          onChange={() => {
            this.setState({
              isSaveAsDefaultChecked: !this.state.isSaveAsDefaultChecked
            });
          }}
        />
        {this.props.children}
      </SaveOpenDialog>
    );

    return (
      <div className="workspace-container">
        <ModalPrompt
          optionalButton={true}
          actionText="Save Changes and Log Out"
          actionCallback={() => {
            this.saveLayoutChangesOnLogout();
          }}
          optionalText="Discard Changes and Log Out"
          optionalCallback={() => {
            this.discardLayoutChangesOnLogout();
          }}
          cancelText="Cancel"
          cancelButtonCallback={() => this.setState({ isSaveLayoutChangesOpen: false })}
          onCloseCallback={() => this.setState({ isSaveLayoutChangesOpen: false })}
          isOpen={this.state.isSaveLayoutChangesOpen}
          title={`Save Changes to \"${this.props.openLayoutName}\"?`}
          actionTooltipText="Saves your changes to the open workspace"
          optionalTooltipText="Discards your changes to the open workspace"
          cancelTooltipText="Cancel and do not log out"
        />
        <ModalPrompt
          optionalButton={true}
          actionText={`Save Changes and Open \"${
            this.state.userLayoutToOpen ? this.state.userLayoutToOpen.name : ''
          }\"`}
          actionCallback={async () => {
            const layout = this.getOpenLayout();
            const variables: UserProfileTypes.SetLayoutMutationArgs = this.createSetLayoutVariables(
              layout
            );
            await this.props.setLayout({ variables }).then(this.openWorkspaceAndSaveState);
          }}
          optionalText={`Discard Changes and Open \"
            ${this.state.userLayoutToOpen ? this.state.userLayoutToOpen.name : ''}\"`}
          optionalCallback={this.openWorkspaceAndSaveState}
          cancelText="Cancel"
          cancelButtonCallback={() =>
            this.setState({
              isSaveWorkspaceOnChangeDialogOpen: false,
              userLayoutToOpen: undefined
            })
          }
          onCloseCallback={() =>
            this.setState({
              isSaveWorkspaceOnChangeDialogOpen: false,
              userLayoutToOpen: undefined
            })
          }
          isOpen={this.state.isSaveWorkspaceOnChangeDialogOpen}
          title={`Save Changes to \"${this.props.openLayoutName}\"?`}
          actionTooltipText="Saves your changes to the open workspace"
          optionalTooltipText="Discards your changes to the open workspace"
          cancelTooltipText="Do not save or open new workspace"
        />
        <Toolbar
          components={this.context.glComponents}
          logo={this.props.logo}
          userName={this.props.userName}
          isAboutDialogOpen={this.state.isAboutDialogOpen}
          isSaveWorkspaceAsDialogOpen={this.state.isSaveWorkspaceAsDialogOpen}
          openLayoutName={this.props.openLayoutName}
          versionInfo={this.props.versionInfo}
          getOpenDisplays={() => this.getOpenDisplays()}
          userProfile={this.props.userProfile}
          saveDialog={saveDialog}
          setLayout={this.props.setLayout}
          setOpenLayoutName={(name: string) => {
            this.props.setOpenLayoutName(name);
          }}
          clearLayout={() => {
            this.clearLayout();
          }}
          logout={() => {
            if (this.isLayoutChanged()) {
              this.setState({ isSaveLayoutChangesOpen: true });
            } else {
              this.finishLogout();
            }
          }}
          openDisplay={displayKey => {
            this.openDisplay(displayKey);
          }}
          openWorkspace={layOut => {
            this.handleOpenWorkspace(layOut);
          }}
          showLogPopup={() => {
            UILogger.Instance().showLogPopUpWindow();
          }}
          showAboutDialog={() => {
            this.toggleAboutDialog();
          }}
          toggleSaveWorkspaceAsDialog={() => this.toggleSaveWorkspaceAsDialog()}
        />
        <div
          className="workspace"
          ref={ref => {
            this.glContainerRef = ref;
          }}
        />
      </div>
    );
  }

  /**
   * On mount, initialize the golden-layout workspace
   */
  public componentDidMount() {
    this.configureGoldenLayout();
    if (this.props.openLayoutName === null) {
      this.props.setOpenLayoutName(this.props.userProfile.defaultLayoutName);
    }
  }

  /**
   * Opens workspace and updates the state
   */
  private readonly openWorkspaceAndSaveState = () => {
    this.openWorkspace(this.state.userLayoutToOpen);
    this.setState({
      userLayoutToOpen: undefined,
      isSaveWorkspaceOnChangeDialogOpen: false
    });
  }

  /**
   * Open a selected workspace layout, adding the layout elements to the GoldenLayout
   * @param userLayout an encoded golden layout
   */
  private handleOpenWorkspace(userLayout: UserProfileTypes.UserLayout) {
    if (this.isLayoutChanged()) {
      this.setState({
        isSaveWorkspaceOnChangeDialogOpen: true,
        userLayoutToOpen: userLayout
      });
    } else {
      this.openWorkspace(userLayout);
    }
  }

  private openWorkspace(userLayout: UserProfileTypes.UserLayout) {
    const newLayout = JSON.parse(decodeURI(userLayout.layoutConfiguration));
    this.gl.root.contentItems.forEach(item => {
      item.remove();
    });
    newLayout.content.forEach(item => {
      this.gl.root.addChild(item);
    });
    this.props.setOpenLayoutName(userLayout.name);
  }

  private getLayoutAsString() {
    return this.gl ? encodeURI(JSON.stringify(this.gl.toConfig())) : undefined;
  }

  /**
   * configure & initialize the golden-layout workspace
   */
  private configureGoldenLayout() {
    if (this.gl) {
      this.destroyGl();
    }
    const savedConfigStr = localStorage.getItem('gms-analyst-ui-layout');
    const defaultConfig: GoldenLayout.Config = this.props.userProfile.workspaceLayouts.find(
      wl => wl.name === this.props.userProfile.defaultLayoutName
    )
      ? JSON.parse(
          decodeURI(
            this.props.userProfile.workspaceLayouts.find(
              wl => wl.name === this.props.userProfile.defaultLayoutName
            ).layoutConfiguration
          )
        )
      : this.context.config.workspace;
    defaultConfig.settings.showPopoutIcon = isElectron();
    UILogger.Instance().log(`Enabling popout window icon for electron: ${isElectron()}`);

    try {
      if (savedConfigStr) {
        try {
          const savedConfig: GoldenLayout.Config = JSON.parse(savedConfigStr);
          savedConfig.settings.showPopoutIcon = isElectron();
          this.gl = new GoldenLayout(savedConfig, this.glContainerRef);
          // if an update has changed the names of components, for example, need to start at default again
        } catch (e) {
          this.gl = new GoldenLayout(defaultConfig, this.glContainerRef);
        }
      } else {
        this.gl = new GoldenLayout(defaultConfig, this.glContainerRef);
      }
    } catch (e) {
      this.gl = new GoldenLayout(this.context.config.workspace, this.glContainerRef);
    }

    const resizeDebounceMillis = 100;

    elementResizeEvent(
      this.glContainerRef,
      debounce(() => {
        this.gl.updateSize();
      }, resizeDebounceMillis)
    );

    this.registerComponentsAndGoldenLayout();

    (this.gl as any).on('stateChanged', () => {
      if (electron !== undefined && electron.ipcRenderer !== undefined) {
        electron.ipcRenderer.send('state-changed');
      }
      if (this.gl.isInitialised) {
        const state = JSON.stringify(this.gl.toConfig());
        localStorage.setItem('gms-analyst-ui-layout', state);
      }
    });
  }

  /**
   * Registers an individual component with golden layout
   *
   * @param name the unique name of the component
   * @param component the component
   */
  private readonly registerComponent = (name: string, component: React.ComponentClass) => {
    this.gl.registerComponent(name, component);
  }

  /**
   * Registers golden layout components and handles error cases
   */
  private readonly registerComponentsAndGoldenLayout = () => {
    try {
      this.registerComponents();
      this.gl.init(); // throws a error if windows can't be popped out
      this.gl.updateSize();
    } catch (e) {
      UILogger.Instance().log(
        'Golden Layout could not initialize. Saved config possibly out of date - Attempting reset to default'
      );
      this.gl = new GoldenLayout(this.context.config.workspace, this.glContainerRef);
      this.registerComponents();
      this.gl.init(); // throws a error if windows can't be popped out
      this.gl.updateSize();
    }
  }

  /** Registers all of the golden layout components */
  private readonly registerComponents = () => {
    this.context.glComponents.forEach((item: GLComponentValue) => {
      if (isGLComponentMap(item)) {
        [...item.values()].forEach((c: GLKeyValue) => {
          this.registerComponent(c.id.component, c.value);
        });
      } else if (isGLKeyValue(item)) {
        this.registerComponent(item.id.component, item.value);
      }
    });
  }

  private readonly destroyGl = () => {
    this.gl.destroy();
  }

  /**
   * Opens display based on menu selection
   * @param componentKey key into the component map for a golden layout configuration
   */
  private readonly openDisplay = (componentKey: string) => {
    if (this.gl.root.contentItems[0]) {
      this.gl.root.contentItems[0].addChild(this.context.config.components[componentKey]);
    } else {
      this.gl.root.addChild(this.context.config.components[componentKey]);
    }
  }

  /**
   * Clears golden layout from local storage
   */
  private readonly clearLayout = () => {
    localStorage.removeItem('gms-analyst-ui-layout');
    location.reload();
  }

  /**
   * Shows or hides the SaveWorkspaceAs dialog
   */
  private toggleSaveWorkspaceAsDialog() {
    this.setState({
      isSaveWorkspaceAsDialogOpen: !this.state.isSaveWorkspaceAsDialogOpen
    });
  }

  /**
   * Shows or hides the AboutDialog
   */
  private toggleAboutDialog() {
    this.setState({
      isAboutDialogOpen: !this.state.isAboutDialogOpen
    });
  }

  /**
   * Saves the current configuration then logs out
   */
  private saveLayoutChangesOnLogout() {
    const newLayout = this.getOpenLayout();
    const variables = this.createSetLayoutVariables(newLayout);
    // The ts lint rule was being incorrectly applied -it is now disabled
    // tslint:disable-next-line: no-floating-promises
    this.props.setLayout({ variables }).then(() => {
      this.finishLogout();
    });
  }

  /**
   * Closes the save layout dialog and finishes logout
   */
  private discardLayoutChangesOnLogout() {
    this.setState({ isSaveLayoutChangesOpen: false });
    this.finishLogout();
  }

  /**
   * Clears local storage and logs out
   */
  private finishLogout() {
    localStorage.removeItem('gms-analyst-ui-layout');
    this.props.logout();
  }

  /**
   * Returns true if the open layout doesn't match the golden layout config
   */
  private isLayoutChanged(): boolean {
    const currentlyOpenLayout = this.props.userProfile.workspaceLayouts.find(
      wl => wl.name === this.props.openLayoutName
    );
    return (
      this.gl &&
      encodeURI(JSON.stringify(this.gl.toConfig())) !== currentlyOpenLayout.layoutConfiguration
    );
  }

  /**
   * Handles the affirmative action for the save dialog.
   */
  private readonly handleAffirmativeAction = async () => {
    if (this.state.saveAsName === '') {
      GoldenLayoutPanel.toaster.toastInfo('Please give a name for the new layout');
    } else {
      await this.submitSaveAs();
    }
  }

  /**
   * Hides the save dialog and saves the layout
   */
  private readonly submitSaveAs = async () => {
    this.toggleSaveWorkspaceAsDialog();
    const layout: UserProfileTypes.UserLayout = {
      name: this.state.saveAsName,
      supportedUserInterfaceModes: [this.context.supportedUserInterfaceMode],
      layoutConfiguration: this.getLayoutAsString()
    };
    const variables: UserProfileTypes.SetLayoutMutationArgs = this.createSetLayoutVariables(layout);
    await this.props.setLayout({ variables });
    this.props.setOpenLayoutName(this.state.saveAsName);
  }

  /**
   * Test
   */
  private readonly createSetLayoutVariables = (
    workspaceLayoutInput: UserProfileTypes.UserLayout = undefined
  ): UserProfileTypes.SetLayoutMutationArgs => ({
    saveAsDefaultLayout: this.state.isSaveAsDefaultChecked
      ? UserProfileTypes.DefaultLayoutNames.SOH_LAYOUT
      : undefined,
    defaultLayoutName: IS_INTERACTIVE_ANALYSIS_MODE_SOH
      ? UserProfileTypes.DefaultLayoutNames.SOH_LAYOUT
      : UserProfileTypes.DefaultLayoutNames.ANALYST_LAYOUT,
    workspaceLayoutInput
  })

  /**
   * Returns the current gl config as a layout
   */
  private readonly getOpenLayout = (): UserProfileTypes.UserLayout => {
    const openLayout = this.props.openLayoutName
      ? this.props.openLayoutName
      : this.props.userProfile.defaultLayoutName;
    if (!this.gl) {
      UILogger.Instance().error('No Golden Layout found while saving layout changes');
    }
    const newLayout: UserProfileTypes.UserLayout = {
      name: openLayout,
      supportedUserInterfaceModes: SUPPORTED_MODES,
      layoutConfiguration: encodeURI(JSON.stringify(this.gl ? this.gl.toConfig() : {}))
    };
    return newLayout;
  }

  /**
   * Gets displays from a generic gl "content" item
   * @param content - may be a component or a collection of components
   */
  private readonly getDisplaysFromContent = (content: any): any[] =>
    content.component
      ? [content.component]
      : content.content
      ? flatMap(content.content, this.getDisplaysFromContent)
      : []

  /**
   * Gets all open components (displays) from a gl config
   * @param glConfig - the configuration to get a component from
   */
  private readonly getDisplaysFromConfig = (glConfig: any): any[] => {
    const content: any[] = glConfig.content;
    const displays = flatMap(content, this.getDisplaysFromContent);
    return displays;
  }

  /**
   * Gets the currently open displays by their component name
   */
  private readonly getOpenDisplays = (): string[] => {
    if (this.gl) {
      const currentConfig = this.gl.toConfig();
      const openDisplays = this.getDisplaysFromConfig(currentConfig);
      return openDisplays;
    }
    return [];
  }
}

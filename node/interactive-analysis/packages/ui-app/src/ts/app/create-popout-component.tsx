import { Classes, Colors } from '@blueprintjs/core';
import { getElectron } from '@gms/ui-util';
import React from 'react';

// electron instance; undefined if not running in electron
const electron = getElectron();

/**
 * Wrap the component with everything it needs to live standalone as a popout
 */
export const createPopoutComponent = (
  Component: any,
  props: any,
  suppressPopinIcon: boolean = false
) => {
  const PopoutComponent = class extends React.Component<any, {}> {
    /**
     * Create the pop-out wrapper component
     */
    public render() {
      return (
        <div
          style={{
            width: '100%',
            height: '100%',
            backgroundColor: Colors.DARK_GRAY2
          }}
          className={Classes.DARK}
        >
          <Component {...this.props} />
          {// only show pop-in button if running in electron
          !suppressPopinIcon &&
          electron &&
          electron !== undefined &&
          electron.ipcRenderer !== undefined ? (
            <div
              className="lm_popin"
              title="pop-in"
              onClick={() => {
                electron.ipcRenderer.send(
                  'popin-window',
                  electron.remote.getCurrentWebContents().popoutConfig
                );
                electron.remote.getCurrentWindow().close();
              }}
            >
              <div className="lm_icon" />
              <div className="lm_bg" />
            </div>
          ) : (
            undefined
          )}
        </div>
      );
    }
  };

  return <PopoutComponent {...props} />;
};

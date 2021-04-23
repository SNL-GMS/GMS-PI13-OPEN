import React from 'react';

// Props include the logo, version of the app, and the current commit SHA
export interface AboutMenuProps {
  logo: any;
  versionNumber: string;
  commitSHA?: string;
}

const HEIGHT_OF_LOGO_PX = 120;

/**
 * The about-menu, which displays the logo, version number, and commit sha if provided.
 */
export const AboutMenu: React.FunctionComponent<AboutMenuProps> = props => (
  <div className="about-menu">
    <div className="about-menu__header">
      <div className="about-menu__logo-container">
        <img src={props.logo} alt="GMS" height={HEIGHT_OF_LOGO_PX} className="about-menu__logo" />
      </div>
      <div className="about-menu__version-container">
        <div className="about-menu__title-container">
          <div className="about-menu__name">GMS</div>
          <div className="about-menu__version">{props.versionNumber}</div>
        </div>
        <div className="about-menu__commit-container">
          {props.commitSHA ? (
            <div className="about-menu__version-bottom-line">
              <span className="about-menu__commit-label">Latest Commit:</span>
              <span className="about-menu__commit">{props.commitSHA}</span>
            </div>
          ) : null}
        </div>
      </div>
    </div>
  </div>
);

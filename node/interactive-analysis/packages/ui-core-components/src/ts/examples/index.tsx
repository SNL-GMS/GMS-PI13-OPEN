import '@blueprintjs/core/src/blueprint.scss';
import '@blueprintjs/datetime/src/blueprint-datetime.scss';
import '../../scss/ui-core-components.scss';
import './style.scss';

import { Button, ButtonGroup, Classes, Colors } from '@blueprintjs/core';
import { IconNames } from '@blueprintjs/icons';
import React from 'react';
import ReactDOM from 'react-dom';
import { HashRouter, Link, Route } from 'react-router-dom';
import { DropDownExample } from './drop-down-example';
import { FilterableOptionListExample } from './filterable-option-list-example';
import { FormNoInputExample } from './form-no-input-example';
import { FormSubmittableExample } from './form-submittable-example';
import { Home } from './home';
import { IntervalPickerExample } from './interval-picker-example';
import { TableExample } from './table-example';
import { TimePickerExample } from './time-picker-example';
import { ToolbarExample } from './toolbar-example';

(window as any).React = React;
(window as any).ReactDOM = ReactDOM;

const App = (): any => (
  <div id="app-content">
    <HashRouter>
      <div
        className={Classes.DARK}
        style={{
          height: '100%',
          width: '100%',
          padding: '0.5rem',
          color: Colors.GRAY4
        }}
      >
        <ButtonGroup minimal={true}>
          <Button icon={IconNames.HOME}>
            <Link to="/">Home</Link>
          </Button>
        </ButtonGroup>

        <hr />
        <Route exact={true} path="/" component={Home} />
        <Route exact={true} path="/Table" component={TableExample} />
        <Route exact={true} path="/FormSubmittable" component={FormSubmittableExample} />
        <Route exact={true} path="/FormNoInput" component={FormNoInputExample} />
        <Route exact={true} path="/DropDownExample" component={DropDownExample} />
        <Route exact={true} path="/IntervalPickerExample" component={IntervalPickerExample} />
        <Route exact={true} path="/TimePickerExample" component={TimePickerExample} />
        <Route exact={true} path="/ToolbarExample" component={ToolbarExample} />
        <Route
          exact={true}
          path="/FilterableOptionListExample"
          component={FilterableOptionListExample}
        />
      </div>
    </HashRouter>
  </div>
);

window.onload = () => {
  ReactDOM.render(<App />, document.getElementById('app'));
};

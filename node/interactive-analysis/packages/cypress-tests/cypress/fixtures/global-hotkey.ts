/// <reference types="Cypress" />
import * as Common from './common';
import { selectors } from './query-selectors';
export const globalHotkey = (key, ctrl, alt, shift) => {
  cy.get(selectors.workspace).type(
    `${ctrl ? Common.ModifierKeys.META : ''}${alt ? Common.ModifierKeys.ALT : ''}${
      shift ? Common.ModifierKeys.SHIFT : ''
    }${key}`
  );
};

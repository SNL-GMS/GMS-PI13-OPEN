import { mount, render, shallow } from 'enzyme';
import toJson from 'enzyme-to-json';

// TODO: Remove this `raf` polyfill once the below issue is sorted
// https://github.com/facebookincubator/create-react-app/issues/3199#issuecomment-332842582
// @see https://medium.com/@barvysta/warning-react-depends-on-requestanimationframe-f498edd404b3
const globalAny: any = global;
export const raf = (globalAny.requestAnimationFrame = cb => {
  setTimeout(cb, 0);
});

// tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
const Enzyme = require('enzyme');
// tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
const Adapter = require('enzyme-adapter-react-16');

// React Enzyme adapter
Enzyme.configure({ adapter: new Adapter() });

// Make Enzyme functions available in all test files without importing
globalAny.shallow = shallow;
globalAny.render = render;
globalAny.mount = mount;
globalAny.toJson = toJson;

// Disable console logs
/* globalAny.console = {
  // disable
  log: jest.fn(), // console.log are ignored in tests
  info: jest.fn(), // console.info are ignored in tests
  debug: jest.fn(), // console.debug are ignored in tests
  warn: jest.fn(), // console.warn are ignored in tests

  // Keep native behavior for these methods
  // tslint:disable-next-line
  error: console.error // console.error are ignored in tests
};
*/

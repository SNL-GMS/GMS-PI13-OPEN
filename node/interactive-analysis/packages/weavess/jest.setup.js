"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
const enzyme_1 = require("enzyme");
const enzyme_to_json_1 = __importDefault(require("enzyme-to-json"));
const globalAny = global;
exports.raf = globalAny.requestAnimationFrame = cb => {
    setTimeout(cb, 0);
};
const Enzyme = require('enzyme');
const Adapter = require('enzyme-adapter-react-16');
Enzyme.configure({ adapter: new Adapter() });
globalAny.shallow = enzyme_1.shallow;
globalAny.render = enzyme_1.render;
globalAny.mount = enzyme_1.mount;
globalAny.toJson = enzyme_to_json_1.default;
//# sourceMappingURL=jest.setup.js.map
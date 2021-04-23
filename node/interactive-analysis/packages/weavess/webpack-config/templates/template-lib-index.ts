// tslint:disable
'use strict';

if (process.env.NODE_ENV === 'production') {
  module.exports = require('./cjs/###NAME###.js');
} else {
  module.exports = require('./cjs/###NAME###.development.js');
}

const { resolve } = require('path');
const root = resolve(__dirname, '.');

module.exports = {
  rootDir: root,
  verbose: true,
  bail: true,
  silent: false,
  cacheDirectory: '<rootDir>/.cache/jest',
  name: 'api-gateway',
  testURL: 'http://localhost/',
  globals: {
    'ts-jest': {
      diagnostics: false
    }
  },
  setupFiles: ['<rootDir>/jest.setup.ts'],
  roots: [
    '<rootDir>/packages/ui-app',
    '<rootDir>/packages/ui-electron',
    // "<rootDir>/packages/api-gateway",
    '<rootDir>/packages/common-graphql',
    '<rootDir>/packages/common-util',
    '<rootDir>/packages/ui-apollo',
    '<rootDir>/packages/ui-core-components',
    '<rootDir>/packages/ui-state',
    '<rootDir>/packages/ui-util',
    '<rootDir>/packages/weavess'
  ],
  projects: [
    '<rootDir>/packages/ui-app/jest.config.js',
    '<rootDir>/packages/ui-electron/jest.config.js',
    // "<rootDir>/packages/api-gateway/jest.config.js",
    '<rootDir>/packages/common-graphql/jest.config.js',
    '<rootDir>/packages/common-util/jest.config.js',
    '<rootDir>/packages/ui-apollo/jest.config.js',
    '<rootDir>/packages/ui-core-components/jest.config.js',
    '<rootDir>/packages/ui-state/jest.config.js',
    '<rootDir>/packages/ui-util/jest.config.js',
    '<rootDir>/packages/weavess/jest.config.js'
  ],
  snapshotSerializers: ['enzyme-to-json/serializer'],
  transform: {
    '^.+\\.jsx?$': 'babel-jest',
    '^.+\\.tsx?$': 'ts-jest'
  },
  testRegex: '/__tests__/.*\\.(test|spec)\\.(ts|tsx)$',
  moduleFileExtensions: ['ts', 'tsx', 'js', 'json'],
  modulePaths: ['./node_modules'],
  moduleDirectories: ['./node_modules'],
  testEnvironment: 'jsdom',
  collectCoverage: true,
  coverageReporters: ['lcov', 'html']
};

const { resolve } = require('path');
const root = resolve(__dirname, '.');

module.exports = {
  rootDir: root,
  verbose: false,
  bail: true,
  silent: true,
  cacheDirectory: '<rootDir>/.cache/jest',
  name: 'ui-app',
  testURL: 'http://localhost/',
  automock: false,
  globals: {
    'ts-jest': {
      diagnostics: false
    }
  },
  setupFiles: ['jsdom-worker', '<rootDir>/jest.setup.ts'],
  snapshotSerializers: ['enzyme-to-json/serializer'],
  transform: {
    '^.+\\.jsx?$': 'babel-jest',
    '^.+\\.tsx?$': 'ts-jest'
  },
  testRegex: '/__tests__/.*\\.(test|spec)\\.(ts|tsx)$',
  moduleFileExtensions: ['ts', 'tsx', 'js', 'json'],
  modulePaths: ['./node_modules'],
  moduleDirectories: ['./node_modules'],
  modulePathIgnorePatterns: [
    '<rootDir>/vscode',
    '<rootDir>/coverage',
    '<rootDir>/dist/',
    '<rootDir>/nginx/',
    '<rootDir>/node_modules/',
    '<rootDir>/resources/'
  ],
  moduleNameMapper: {
    '~src/(.*)': '<rootDir>/src/ts/$1',
    '~css/(.*)': '<rootDir>/src/css/$1',
    '~app/(.*)': '<rootDir>/src/ts/app/$1',
    '~components/(.*)': '<rootDir>/src/ts/components/$1',
    '~analyst-ui/(.*)': '<rootDir>/src/ts/components/analyst-ui/$1',
    '~data-acquisition-ui/(.*)': '<rootDir>/src/ts/components/data-acquisition-ui/$1',
    '~common-ui/(.*)': '<rootDir>/src/ts/components/common-ui/$1',
    '~config/(.*)': '<rootDir>/src/ts/config/$1',
    '~resources/(.*)': '<rootDir>/src/ts/resources/$1',
    '~scss-config/(.*)': '<rootDir>/__mocks__/$1',
    '@gms/([^/]+)$': '<rootDir>/../$1/src/ts/$1',
    '@gms/([^/]+)(/lib/)(.*)$': '<rootDir>/../$1/src/ts/$3',
    '^worker-loader*': '<rootDir>/node_modules/worker-loader',
    '.*\\.(css|less|styl|scss|sass)$': '<rootDir>/__mocks__/style-mock.ts',
    '.*\\.(jpg|jpeg|png|gif|eot|otf|webp|svg|ttf|woff|woff2|mp4|webm|wav|mp3|m4a|aac|oga)$':
      '<rootDir>/__mocks__/file-mock.ts'
  },
  testEnvironment: 'jest-environment-jsdom-fourteen',
  collectCoverage: true,
  coverageReporters: ['lcov', 'html']
};

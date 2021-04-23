const { resolve } = require('path');
const root = resolve(__dirname, '.');

module.exports = {
  rootDir: root,
  verbose: true,
  bail: true,
  cacheDirectory: '<rootDir>/.cache/jest-integration',
  name: 'interactive-analysis-api-gateway',
  testURL: 'http://localhost/',
  globals: {
    'ts-jest': {
      diagnostics: false
    }
  },
  transform: {
    '^.+\\.jsx?$': 'babel-jest',
    '^.+\\.tsx?$': 'ts-jest'
  },
  testRegex: '/__integration-tests__/.*\\.(ts|tsx)$',
  testPathIgnorePatterns: ['/node_modules/', '/util/'],
  moduleFileExtensions: ['ts', 'tsx', 'js', 'json'],
  modulePaths: ['./node_modules'],
  moduleDirectories: ['./node_modules'],
  moduleNameMapper: {
    '.*\\.(css|less|styl|scss|sass)$': '<rootDir>/__mocks__/style-mock.ts',
    '.*\\.(jpg|jpeg|png|gif|eot|otf|webp|svg|ttf|woff|woff2|mp4|webm|wav|mp3|m4a|aac|oga)$':
      '<rootDir>/__mocks__/file-mock.ts'
  },
  testEnvironment: 'node',
  collectCoverage: true,
  coverageReporters: ['lcov', 'html']
};

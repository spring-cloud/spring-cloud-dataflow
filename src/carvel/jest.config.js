module.exports = {
  clearMocks: true,
  bail: 1,
  moduleFileExtensions: ['js', 'ts'],
  testEnvironment: 'node',
  testMatch: ['**/*.test.ts'],
  testRunner: 'jest-circus/runner',
  transform: {
    '^.+\\.ts$': 'ts-jest'
  },
  verbose: true,
  setupFilesAfterEnv: ['jest-extended', 'jest-expect-message']
}
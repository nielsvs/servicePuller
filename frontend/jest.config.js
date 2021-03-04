module.exports = {
  setupFilesAfterEnv: ["./jest-dom.ts"],
  roots: ["src"],
  moduleDirectories: ["node_modules", "src"],
  moduleFileExtensions: ["js", "jsx", "ts", "tsx"],
  testMatch: [
    "**/__tests__/**/*.+(ts|tsx|js)",
    "**/?(*.)+(spec|test).+(ts|tsx|js)",
  ],
  moduleNameMapper: {
    "\\.(png|jpg)$": "<rootDir>/empty-file-mock.js",
  },
  transform: {
    "^.+\\.(ts|tsx)$": "ts-jest",
  },
};

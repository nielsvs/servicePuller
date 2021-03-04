const path = require("path");
const ReactRefreshPlugin = require("@pmmmwh/react-refresh-webpack-plugin");
const ForkTsCheckerWebpackPlugin = require("fork-ts-checker-webpack-plugin");
const HtmlWebpackPlugin = require("html-webpack-plugin");
const TsconfigPathsPlugin = require("tsconfig-paths-webpack-plugin");

const isDevelopment = process.env.NODE_ENV !== "production";

module.exports = {
  mode: isDevelopment ? "development" : "production",
  entry: {
    main: "./src/index.tsx",
  },
  module: {
    rules: [
      {
        test: /\.tsx?$/,
        include: path.join(__dirname, "src"),
        use: [
          isDevelopment && {
            loader: "babel-loader",
            options: { plugins: ["react-refresh/babel"] },
          },
          {
            loader: "ts-loader",
            options: { transpileOnly: true },
          },
        ].filter(Boolean),
      },
      {
        test: /\.(jpg|png)$/,
        use: {
          loader: "url-loader",
        },
      },
    ],
  },
  devServer: {
    hot: true,
    headers: {
      "Access-Control-Allow-Origin": "*",
    },
  },
  plugins: [
    isDevelopment && new ReactRefreshPlugin(),
    new ForkTsCheckerWebpackPlugin(),
    new HtmlWebpackPlugin({
      filename: "./index.html",
      template: "./public/index.html",
    }),
  ].filter(Boolean),
  resolve: {
    extensions: [".js", ".ts", ".tsx"],
    plugins: [new TsconfigPathsPlugin()],
  },
};

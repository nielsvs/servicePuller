# Service Poller

Keep track of the health of your services. Add a url and watch as the service will be marked as OK or FAIl.

## How to configure

Running the following command will configure the application with the necessary dependencies:

```
npm i
```

## How to run locally

The application can be run locally with the following command:

```
npm start
```

It utilizes `webpack-dev-server` and it supports `react-refresh` for developing without losing state on refreshes.

## How to build for production

The application uses a environment variable to determine which environment the application should be build towards. By running the following command, the application will be packaged in the `dist` folder:

```
npm run build
```

The content of the `dist` folder can be utilized to host the application
as a static website.

## Tests

The application can be tested using the following command:

```
npm test
```

This will run all tests in the `src` folder and it will watch for any changes to either tests or the imports of the tests to determine if the test should be re-run.

## Technologies

The following technologies are the mainly used technologies to build, package and test the application:

- [ReactJS](https://reactjs.org/)
- [styled-components](https://styled-components.com/)
- [Typescript](https://www.typescriptlang.org/)
- [Webpack](https://webpack.js.org/)
- [Jest](https://jestjs.io/)
- [React Testing Library](https://testing-library.com/)

## Requirements

- Node 14.15+
- npm 6.14+

## TODO

- Add production apiHost when available
- Improve error messages to users (could come from BE)
- Move local text to CMS

## Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

Please make sure to update tests as appropriate.

## License

[MIT](https://choosealicense.com/licenses/mit/)

import React from "react";
import { createGlobalStyle } from "styled-components";

const GlobalStyle = createGlobalStyle`
  body {
    height: 100%;
    margin: 0;
    font-family: "Roboto";
  }
  html {
    height: 100%;
  }
`;

function App() {
  return (
    <div>
      <GlobalStyle />
      <React.Suspense fallback={<h1>Loading</h1>}>
        <h1>Hello</h1>
      </React.Suspense>
    </div>
  );
}

export default App;

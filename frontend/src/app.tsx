import React from "react";
import { createGlobalStyle } from "styled-components";
import Overview from "pages/Overview";

const GlobalStyle = createGlobalStyle`
  body {
    font-family: "Roboto";
  }
`;

function App() {
  return (
    <div>
      <GlobalStyle />
      <React.Suspense fallback={<h1>Loading</h1>}>
        <Overview />
      </React.Suspense>
    </div>
  );
}

export default App;

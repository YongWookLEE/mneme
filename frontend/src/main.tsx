import React from "react";
import ReactDOM from "react-dom/client";
import App from "./App";
import "./styles/globals.css";

/**
 * 프론트엔드 진입점.
 *
 * React 18 createRoot로 #root에 App을 마운트한다.
 * 라우터/QueryClient/i18n Provider는 phase 03(인증) 이후에 추가한다.
 */
const container = document.getElementById("root");
if (!container) {
  throw new Error("root container not found");
}

ReactDOM.createRoot(container).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
);

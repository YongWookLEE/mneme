import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import App from "../App";

/**
 * 앱 루트 컴포넌트 스모크 테스트.
 *
 * 미인증 상태(localStorage 비어 있음 + jsdom의 localStorage stub)에서는 AuthGate가
 * "Mneme 대시보드" 입력 화면을 노출한다. 부팅 자체 검증.
 */
describe("App", () => {
  it("AuthGate 입력 화면이 렌더된다", () => {
    render(<App />);
    expect(screen.getByRole("heading", { name: /Mneme 대시보드/i })).toBeInTheDocument();
  });
});

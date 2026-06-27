import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import App from "../App";

/**
 * 앱 루트 컴포넌트 스모크 테스트.
 *
 * Phase 01에서는 "Mneme" 텍스트가 표시되는 것이 곧 부팅 성공 신호.
 */
describe("App", () => {
  it("Mneme 타이틀이 렌더된다", () => {
    render(<App />);
    expect(screen.getByRole("heading", { name: /Mneme/i })).toBeInTheDocument();
  });
});

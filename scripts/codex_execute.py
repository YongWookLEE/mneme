#!/usr/bin/env python3
"""
Codex Harness Step Executor — phase 내 step을 Codex로 순차 실행하고 자가 교정한다.

Usage:
    python3 scripts/codex_execute.py <phase-dir> [--push]
"""

import argparse
import json
import subprocess
import sys
from pathlib import Path

import execute
from execute import StepExecutor


ROOT = Path(__file__).resolve().parent.parent


class CodexStepExecutor(StepExecutor):
    """StepExecutor의 상태 관리 흐름은 유지하고 Codex 호출부만 교체한다."""

    AGENT_NAME = "Codex"

    def __init__(self, phase_dir_name: str, *, auto_push: bool = False):
        self._sync_execute_root()
        super().__init__(phase_dir_name, auto_push=auto_push)

    @staticmethod
    def _sync_execute_root():
        execute.ROOT = ROOT

    def _load_guardrails(self) -> str:
        sections = []
        agents_md = ROOT / "AGENTS.md"
        claude_md = ROOT / "CLAUDE.md"

        if agents_md.exists():
            sections.append(f"## 프로젝트 규칙 (AGENTS.md)\n\n{agents_md.read_text(encoding='utf-8')}")
        elif claude_md.exists():
            sections.append(f"## 프로젝트 규칙 (CLAUDE.md)\n\n{claude_md.read_text(encoding='utf-8')}")

        docs_dir = ROOT / "docs"
        if docs_dir.is_dir():
            for doc in sorted(docs_dir.glob("*.md")):
                sections.append(f"## {doc.stem}\n\n{doc.read_text(encoding='utf-8')}")
        return "\n\n---\n\n".join(sections) if sections else ""

    def _invoke_agent(self, step: dict, preamble: str) -> dict:
        return self._invoke_codex(step, preamble)

    def _invoke_codex(self, step: dict, preamble: str) -> dict:
        step_num, step_name = step["step"], step["name"]
        step_file = self._phase_dir / f"step{step_num}.md"

        if not step_file.exists():
            print(f"  ERROR: {step_file} not found")
            sys.exit(1)

        prompt = preamble + step_file.read_text(encoding="utf-8")
        result = subprocess.run(
            [
                "codex",
                "exec",
                "--cd",
                str(ROOT),
                "--sandbox",
                "danger-full-access",
                "--config",
                'approval_policy="never"',
                "--json",
                "-",
            ],
            cwd=self._root,
            input=prompt,
            capture_output=True,
            text=True,
            timeout=1800,
        )

        if result.returncode != 0:
            print(f"\n  WARN: Codex가 비정상 종료됨 (code {result.returncode})")
            if result.stderr:
                print(f"  stderr: {result.stderr[:500]}")

        output = {
            "step": step_num,
            "name": step_name,
            "exitCode": result.returncode,
            "stdout": result.stdout,
            "stderr": result.stderr,
        }
        out_path = self._phase_dir / f"step{step_num}-output.json"
        with open(out_path, "w", encoding="utf-8") as f:
            json.dump(output, f, indent=2, ensure_ascii=False)

        return output


def main():
    parser = argparse.ArgumentParser(description="Codex Harness Step Executor")
    parser.add_argument("phase_dir", help="Phase directory name (e.g. 0-mvp)")
    parser.add_argument("--push", action="store_true", help="Push branch after completion")
    args = parser.parse_args()

    CodexStepExecutor(args.phase_dir, auto_push=args.push).run()


if __name__ == "__main__":
    main()

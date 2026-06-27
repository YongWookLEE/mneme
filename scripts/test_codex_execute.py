import json
import subprocess
import sys
from pathlib import Path
from unittest.mock import MagicMock, patch

import pytest

sys.path.insert(0, str(Path(__file__).parent))
import codex_execute as ce


@pytest.fixture
def tmp_project(tmp_path):
    phases_dir = tmp_path / "phases"
    phases_dir.mkdir()

    (tmp_path / "AGENTS.md").write_text("# Agents\n- codex rule", encoding="utf-8")
    (tmp_path / "CLAUDE.md").write_text("# Claude\n- fallback rule", encoding="utf-8")

    docs_dir = tmp_path / "docs"
    docs_dir.mkdir()
    (docs_dir / "arch.md").write_text("# Architecture\nSome content", encoding="utf-8")
    (docs_dir / "guide.md").write_text("# Guide\nAnother doc", encoding="utf-8")

    return tmp_path


@pytest.fixture
def phase_dir(tmp_project):
    d = tmp_project / "phases" / "0-mvp"
    d.mkdir()
    index = {
        "project": "TestProject",
        "phase": "mvp",
        "steps": [
            {"step": 0, "name": "setup", "status": "completed", "summary": "done"},
            {"step": 1, "name": "core", "status": "pending"},
        ],
    }
    (d / "index.json").write_text(json.dumps(index, indent=2), encoding="utf-8")
    (d / "step1.md").write_text("# Step 1\n\nBuild core.", encoding="utf-8")
    return d


@pytest.fixture
def executor(tmp_project, phase_dir):
    with patch.object(ce, "ROOT", tmp_project):
        inst = ce.CodexStepExecutor("0-mvp")
    inst._root = str(tmp_project)
    inst._phases_dir = tmp_project / "phases"
    inst._phase_dir = phase_dir
    inst._phase_dir_name = "0-mvp"
    inst._index_file = phase_dir / "index.json"
    inst._top_index_file = tmp_project / "phases" / "index.json"
    return inst


class TestCodexGuardrails:
    def test_loads_agents_and_docs(self, executor, tmp_project):
        with patch.object(ce, "ROOT", tmp_project):
            result = executor._load_guardrails()

        assert "# Agents" in result
        assert "codex rule" in result
        assert "# Architecture" in result
        assert "# Guide" in result

    def test_agents_preferred_over_claude(self, executor, tmp_project):
        with patch.object(ce, "ROOT", tmp_project):
            result = executor._load_guardrails()

        assert "AGENTS.md" in result
        assert "CLAUDE.md" not in result
        assert "fallback rule" not in result

    def test_claude_fallback_when_agents_missing(self, executor, tmp_project):
        (tmp_project / "AGENTS.md").unlink()
        with patch.object(ce, "ROOT", tmp_project):
            result = executor._load_guardrails()

        assert "CLAUDE.md" in result
        assert "fallback rule" in result


class TestInvokeCodex:
    def test_invokes_codex_with_correct_args_and_stdin(self, executor, tmp_project):
        mock_result = MagicMock(returncode=0, stdout='{"result": "ok"}', stderr="")
        step = {"step": 1, "name": "core"}

        with patch.object(ce, "ROOT", tmp_project):
            with patch("subprocess.run", return_value=mock_result) as mock_run:
                output = executor._invoke_codex(step, "PREAMBLE\n")

        cmd = mock_run.call_args[0][0]
        kwargs = mock_run.call_args[1]

        assert cmd == [
            "codex",
            "exec",
            "--cd",
            str(tmp_project),
            "--sandbox",
            "danger-full-access",
            "--config",
            'approval_policy="never"',
            "--json",
            "-",
        ]
        assert kwargs["input"].startswith("PREAMBLE\n")
        assert "Build core." in kwargs["input"]
        assert kwargs["timeout"] == 1800
        assert output["exitCode"] == 0

    def test_saves_output_json_same_shape(self, executor):
        mock_result = MagicMock(returncode=7, stdout="out", stderr="err")
        step = {"step": 1, "name": "core"}

        with patch("subprocess.run", return_value=mock_result):
            executor._invoke_codex(step, "preamble")

        data = json.loads((executor._phase_dir / "step1-output.json").read_text(encoding="utf-8"))
        assert data == {
            "step": 1,
            "name": "core",
            "exitCode": 7,
            "stdout": "out",
            "stderr": "err",
        }

    def test_nonexistent_step_file_exits(self, executor):
        with pytest.raises(SystemExit) as exc_info:
            executor._invoke_codex({"step": 99, "name": "missing"}, "preamble")
        assert exc_info.value.code == 1


class TestCli:
    def test_no_args_exits(self):
        with patch("sys.argv", ["codex_execute.py"]):
            with pytest.raises(SystemExit) as exc_info:
                ce.main()
        assert exc_info.value.code == 2

    def test_invalid_phase_dir_exits(self):
        with patch("sys.argv", ["codex_execute.py", "nonexistent"]):
            with patch.object(ce, "ROOT", Path("/tmp/fake_nonexistent")):
                with pytest.raises(SystemExit) as exc_info:
                    ce.main()
        assert exc_info.value.code == 1


class TestHooks:
    def test_dangerous_guard_blocks_dangerous_command(self):
        hook = Path(__file__).parent / "hooks" / "codex-dangerous-cmd-guard.py"
        payload = json.dumps({"tool_input": {"cmd": "rm -rf dist"}})
        result = subprocess.run([sys.executable, str(hook)], input=payload, text=True, capture_output=True)
        assert result.returncode != 0

    def test_dangerous_guard_allows_safe_command(self):
        hook = Path(__file__).parent / "hooks" / "codex-dangerous-cmd-guard.py"
        payload = json.dumps({"tool_input": {"cmd": "npm test"}})
        result = subprocess.run([sys.executable, str(hook)], input=payload, text=True, capture_output=True)
        assert result.returncode == 0

    def test_stop_check_skips_without_package_json(self, tmp_path):
        hook = Path(__file__).parent / "hooks" / "codex-stop-check.sh"
        result = subprocess.run([str(hook)], cwd=tmp_path, text=True, capture_output=True)
        assert result.returncode == 0
        assert "skipping" in result.stdout

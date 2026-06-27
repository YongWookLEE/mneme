#!/usr/bin/env python3
"""Block dangerous shell commands from Codex/Claude hook payloads."""

import json
import os
import re
import sys
from typing import Any


PATTERNS = [
    re.compile(r"\brm\s+-[^\n;|&]*r[^\n;|&]*f\b|\brm\s+-[^\n;|&]*f[^\n;|&]*r\b", re.IGNORECASE),
    re.compile(r"\bgit\s+push\s+--force(?:\b|=)", re.IGNORECASE),
    re.compile(r"\bgit\s+reset\s+--hard\b", re.IGNORECASE),
    re.compile(r"\bDROP\s+TABLE\b", re.IGNORECASE),
]


def extract_strings(value: Any):
    if isinstance(value, str):
        yield value
    elif isinstance(value, dict):
        for item in value.values():
            yield from extract_strings(item)
    elif isinstance(value, list):
        for item in value:
            yield from extract_strings(item)


def main() -> int:
    raw = sys.stdin.read()
    env_payload = os.environ.get("CLAUDE_TOOL_INPUT", "")
    if env_payload:
        raw = f"{raw}\n{env_payload}"
    if not raw.strip():
        return 0

    try:
        payload = json.loads(raw)
        text = "\n".join(extract_strings(payload))
    except json.JSONDecodeError:
        text = raw

    for pattern in PATTERNS:
        if pattern.search(text):
            print("BLOCKED: dangerous command detected.", file=sys.stderr)
            return 1

    return 0


if __name__ == "__main__":
    sys.exit(main())

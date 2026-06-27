#!/usr/bin/env bash
set -euo pipefail

if [ ! -f package.json ]; then
  echo "harness-stop-check: package.json not found; skipping npm checks."
  exit 0
fi

has_script() {
  local script="$1"
  node -e "const s=require('./package.json').scripts||{}; process.exit(Object.prototype.hasOwnProperty.call(s, process.argv[1]) ? 0 : 1)" "$script"
}

run_if_exists() {
  local script="$1"
  if has_script "$script"; then
    npm run "$script"
  else
    echo "harness-stop-check: npm script '$script' not found; skipping."
  fi
}

run_if_exists lint
run_if_exists build
run_if_exists test

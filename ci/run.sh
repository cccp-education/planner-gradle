#!/usr/bin/env bash
# GBL-013 US-5 (wave 2) — CI contract for planner-gradle.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

job="${1:-}"
case "$job" in
  fast|test)
    ./gradlew :planner-plugin:build --no-daemon --console=plain
    ;;
  *)
    printf 'unsupported job for planner-gradle: %s\n' "${job:-<none>}" >&2
    printf 'supported: fast | test\n' >&2
    exit 2
    ;;
esac

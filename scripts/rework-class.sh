#!/usr/bin/env bash
# Thin shim around the :reworkClass Gradle task — the only rework script.
# Usage: ./scripts/rework-class.sh <path/to/File.java> [SUGGEST_ONLY|AGENT_DRIVEN]
set -euo pipefail
exec ./gradlew reworkClass -Pfile="$1" -Pmode="${2:-SUGGEST_ONLY}"

#!/usr/bin/env bash
#
# Wrapper used by crontab entries that runs scripts/run-experiment.sh with the
# environment the real Gradle/OpenRewrite/Claude CLI stack needs. Cron runs
# with a minimal PATH and no JAVA_HOME, so those are set explicitly here.
#
# Usage: scripts/cron-run-experiment.sh <manual|recipe> <run-number>

set -euo pipefail

export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$HOME/.local/bin:/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin"

REPO="/Users/pippanewbold/CleanClaude"
APPROACH=$1
RUN=$2
LOG_DIR="$REPO/experiment/cron-logs"
mkdir -p "$LOG_DIR"
LOG="$LOG_DIR/${APPROACH}-${RUN}-$(date +%Y%m%d-%H%M%S).log"

{
    echo "=== cron wrapper starting $(date) ==="
    echo "approach=$APPROACH run=$RUN"
    echo "JAVA_HOME=$JAVA_HOME"
    echo "PATH=$PATH"
    echo "pwd=$(pwd)"
    echo
    cd "$REPO"
    bash scripts/run-experiment.sh "$APPROACH" "$RUN"
    echo
    echo "=== cron wrapper finished $(date) exit=$? ==="
} >> "$LOG" 2>&1

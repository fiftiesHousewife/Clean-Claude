#!/usr/bin/env bash
#
# Self-applies the Clean Code plugin to this codebase and refreshes
# docs/reports/. The single command contributors should run before opening a
# PR; CI also runs it and fails if docs/reports/ drifts from the committed
# state.
#
# Steps:
#   1. Publish all modules to ~/.m2/repository so the dogfood init script can
#      resolve `io.github.fiftieshousewife.cleancode:plugin:1.0-SNAPSHOT`.
#   2. Run analyseCleanCode against every Java module via the init script
#      (no committed build-file changes — `./gradlew build` stays plugin-free).
#   3. Copy each module's build/reports/clean-code/findings.html into
#      docs/reports/<module>.html so the committed report set matches the
#      links emitted by cleanCodeSummary.
#   4. Run cleanCodeSummary to regenerate docs/reports/index.html.
#
# Usage: scripts/dogfood.sh

set -euo pipefail

REPO="$(git rev-parse --show-toplevel)"
cd "$REPO"

INIT_SCRIPT="$REPO/scripts/cleancode-dogfood.init.gradle.kts"
REPORTS_DIR="$REPO/docs/reports"

JAVA_MODULES=(annotations core adapters claude-review plugin recipes refactoring)

# Single source of truth for the plugin version is the root build.gradle.kts.
# Parsed once and threaded through to the init script so coordinates never
# drift between publishToMavenLocal and the dogfood self-apply.
PLUGIN_VERSION=$(grep -E '^version\s*=\s*"' "$REPO/build.gradle.kts" | head -n1 \
    | sed -E 's/.*"([^"]+)".*/\1/')
if [[ -z "$PLUGIN_VERSION" ]]; then
    echo "[dogfood] could not parse version from build.gradle.kts" >&2
    exit 1
fi
echo "[dogfood] plugin version: $PLUGIN_VERSION"

echo "[dogfood] publishing plugin + siblings to mavenLocal"
./gradlew --quiet publishToMavenLocal

echo "[dogfood] running analyseCleanCode across all modules via init script"
# --continue so a single module's PMD or JaCoCo failure doesn't drop the rest
# of the sweep. Modules that fail produce no findings.html for this run; the
# copy step below logs WARN and the summary table reflects the partial state.
set +e
./gradlew --console=plain \
    -DcleanCodePluginVersion="$PLUGIN_VERSION" \
    --init-script "$INIT_SCRIPT" \
    --continue \
    analyseCleanCode
ANALYSE_RC=$?
set -e
if [[ $ANALYSE_RC -ne 0 ]]; then
    echo "[dogfood] analyseCleanCode finished with rc=$ANALYSE_RC — proceeding with whichever reports exist"
fi

echo "[dogfood] copying per-module findings.html into docs/reports/"
mkdir -p "$REPORTS_DIR"

copy_report() {
    local module=$1
    local source=$2
    local target="$REPORTS_DIR/${module}.html"
    if [[ ! -f "$source" ]]; then
        echo "[dogfood] WARN: missing $source — skipping ${module}.html"
        return
    fi
    cp "$source" "$target"
}

copy_report "root" "$REPO/build/reports/clean-code/findings.html"
for module in "${JAVA_MODULES[@]}"; do
    copy_report "$module" "$REPO/${module}/build/reports/clean-code/findings.html"
done

echo "[dogfood] regenerating summary index"
./gradlew --quiet \
    -DcleanCodePluginVersion="$PLUGIN_VERSION" \
    --init-script "$INIT_SCRIPT" \
    cleanCodeSummary

echo "[dogfood] verifying SUMMARY drift (same check as CI)"
if ! git diff --exit-code -- docs/reports/SUMMARY.md docs/reports/index.html; then
    echo "[dogfood] ERROR: docs/reports/SUMMARY.md or index.html drifted from the committed state."
    echo "[dogfood] Review the diff above, then 'git add docs/reports/SUMMARY.md docs/reports/index.html' and commit."
    exit 1
fi

echo "[dogfood] done — see docs/reports/index.html"

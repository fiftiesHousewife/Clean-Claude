#!/usr/bin/env bash
#
# Wrapper around ./gradlew that injects the sandbox HTTP proxy as JVM
# system properties so Java's default HTTP client honours it. Required
# because the sandbox sets `http_proxy`/`https_proxy` env vars (which
# curl, gh, npm, etc. read automatically) but the JVM does not — without
# these `-D` flags, gradle dependency resolution fails with
# `nodename nor servname provided` or `Read timed out`.
#
# Picks up the proxy URL at invocation time from $https_proxy (or
# $HTTPS_PROXY); the sandbox port differs per session, so don't hardcode.
#
# Usage:
#   scripts/gradle-with-proxy.sh <gradle-args...>
#
# Example:
#   scripts/gradle-with-proxy.sh prefetchPhaseA \
#       -I scripts/prefetch-phase-a.init.gradle.kts

set -euo pipefail

REPO="$(git rev-parse --show-toplevel)"
PROXY_URL="${https_proxy:-${HTTPS_PROXY:-}}"

if [[ -z "$PROXY_URL" ]]; then
    echo "[gradle-with-proxy] no https_proxy / HTTPS_PROXY env var set; running gradle directly" >&2
    exec "$REPO/gradlew" "$@"
fi

PROXY_HOSTPORT="${PROXY_URL#*://}"
PROXY_HOSTPORT="${PROXY_HOSTPORT%/}"
PROXY_HOST="${PROXY_HOSTPORT%:*}"
PROXY_PORT="${PROXY_HOSTPORT##*:}"

if [[ -z "$PROXY_HOST" || -z "$PROXY_PORT" || "$PROXY_HOST" == "$PROXY_PORT" ]]; then
    echo "[gradle-with-proxy] could not parse host:port from '$PROXY_URL'" >&2
    exit 1
fi

echo "[gradle-with-proxy] using JVM proxy ${PROXY_HOST}:${PROXY_PORT}" >&2

exec "$REPO/gradlew" \
    "-Dhttps.proxyHost=${PROXY_HOST}" "-Dhttps.proxyPort=${PROXY_PORT}" \
    "-Dhttp.proxyHost=${PROXY_HOST}" "-Dhttp.proxyPort=${PROXY_PORT}" \
    "$@"
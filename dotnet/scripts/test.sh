#!/usr/bin/env bash
# Levanta el mock de conformidad y corre la suite de .NET contra él.
#   scripts/test.sh                                  # build net8.0
#   scripts/test.sh -p:KuidaTarget=netstandard2.0    # build netstandard2.0
set -euo pipefail
HERE="$(cd "$(dirname "$0")/.." && pwd)"
PORT="${PORT:-12123}"
export KUIDA_API_BASE="http://localhost:${PORT}/api"

PORT="$PORT" node "$HERE/../conformance/mock-server.mjs" >/dev/null 2>&1 &
MOCK=$!
trap 'kill $MOCK 2>/dev/null || true' EXIT
for _ in $(seq 1 50); do curl -s "http://localhost:${PORT}/__mock/requests" >/dev/null && break; sleep 0.1; done

cd "$HERE"
dotnet test --nologo "$@"

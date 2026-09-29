#!/usr/bin/env bash
# Corre la suite de conformidad de un SDK (o de todos) contra el mock.
#   conformance/run.sh node|python|all
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PORT="${PORT:-12111}"
export KUIDA_API_BASE="http://localhost:${PORT}/api"

node "$ROOT/conformance/mock-server.mjs" >/tmp/kuida-mock.log 2>&1 &
MOCK=$!
trap 'kill $MOCK 2>/dev/null || true' EXIT
for _ in $(seq 1 50); do curl -s "http://localhost:${PORT}/__mock/requests" >/dev/null && break; sleep 0.1; done

run() {
  echo "=== $1 ==="
  case "$1" in
    node)   (cd "$ROOT/node" && npm test --silent) ;;
    python) (cd "$ROOT/python" && PY=python3 && [[ -x .venv/bin/python ]] && PY=.venv/bin/python; "$PY" -m pytest -q) ;;
    *) echo "lenguaje desconocido: $1"; exit 2 ;;
  esac
}

if [[ "${1:-all}" == "all" ]]; then
  for l in node python; do run "$l"; done
else
  run "$1"
fi

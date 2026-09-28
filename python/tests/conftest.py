"""Fixtures de la suite de conformidad (SDK_DESIGN.md §9).

Necesita el mock corriendo y ``KUIDA_API_BASE`` apuntando a él::

    PORT=12122 node ../conformance/mock-server.mjs &
    KUIDA_API_BASE=http://localhost:12122/api python3 -m pytest -q
"""

import json
import os
from pathlib import Path
from typing import Any, Dict, List

import httpx
import pytest

import kuida

API_BASE = os.environ.get("KUIDA_API_BASE", "http://localhost:12111/api").rstrip("/")
MOCK_ROOT = API_BASE[: -len("/api")] if API_BASE.endswith("/api") else API_BASE
VECTORS_PATH = Path(__file__).resolve().parents[2] / "conformance" / "webhook-vectors.json"

KEY_OK = "kd_test_000000000000_mocksecretmocksecret00"
KEY_EVENTS_ONLY = "kd_test_1e9ac7000000_mocksecretmocksecret00"
KEY_REVOKED = "kd_test_dead00000000_mocksecretmocksecret00"
KEY_FLAKY_503 = "kd_test_fa11ed000000_mocksecretmocksecret00"
KEY_FLAKY_429 = "kd_test_42900000000a_mocksecretmocksecret00"


class Mock:
    """Acceso a los endpoints de control del mock."""

    def reset(self) -> None:
        httpx.post(MOCK_ROOT + "/__mock/reset").raise_for_status()

    def requests(self) -> List[Dict[str, Any]]:
        res = httpx.get(MOCK_ROOT + "/__mock/requests")
        res.raise_for_status()
        return res.json()["requests"]


class SleepRecorder:
    """Espera nula inyectable: registra cuánto habría esperado el SDK."""

    def __init__(self) -> None:
        self.calls: List[float] = []

    def __call__(self, seconds: float) -> None:
        self.calls.append(seconds)


@pytest.fixture(scope="session", autouse=True)
def _mock_up() -> None:
    try:
        httpx.get(MOCK_ROOT + "/__mock/requests", timeout=2)
    except httpx.HTTPError:
        pytest.exit("El mock no responde en %s. Levantalo con node conformance/mock-server.mjs" % MOCK_ROOT, 2)


@pytest.fixture
def mock() -> Mock:
    m = Mock()
    m.reset()
    return m


@pytest.fixture
def sleeps() -> SleepRecorder:
    return SleepRecorder()


@pytest.fixture
def make_client(sleeps: SleepRecorder):
    clients: List[kuida.Kuida] = []

    def factory(api_key: str = KEY_OK, **kwargs: Any) -> kuida.Kuida:
        kwargs.setdefault("base_url", API_BASE)
        kwargs.setdefault("sleep", sleeps)
        c = kuida.Kuida(api_key=api_key, **kwargs)
        clients.append(c)
        return c

    yield factory
    for c in clients:
        c.close()


@pytest.fixture
def client(make_client) -> kuida.Kuida:
    return make_client()


@pytest.fixture
def vectors() -> Dict[str, Any]:
    return json.loads(VECTORS_PATH.read_text(encoding="utf-8"))

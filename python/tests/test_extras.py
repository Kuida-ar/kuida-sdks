"""Pruebas adicionales: cliente async, conversión de parámetros y modelos."""

import asyncio
import pickle
from datetime import date, datetime, timedelta, timezone

import pytest

import kuida
from kuida._params import body_params, query_params
from kuida.errors import InvalidRequestError

from conftest import API_BASE, KEY_FLAKY_429, KEY_OK


def phone(n: int) -> str:
    return "+54 9 342 555 %04d" % n


# ─── async ───────────────────────────────────────────────────────────────────


def test_async_client_surface(mock):
    sleeps = []

    async def fake_sleep(seconds: float) -> None:
        sleeps.append(seconds)

    async def run():
        async with kuida.AsyncKuida(api_key=KEY_OK, base_url=API_BASE, sleep=fake_sleep) as client:
            account = await client.account.retrieve()
            assert account.object == "account"
            ids = set()
            for i in range(5):
                p = await client.patients.create(phone=phone(100 + i), full_name="Paciente Demo %d" % i)
                ids.add(p.id)
            page = await client.patients.list(limit=2)
            assert len(page) == 2 and page.has_more
            seen = [p.id async for p in page.auto_paging_iter()]
            assert set(seen) == ids and len(seen) == 5
            apt = await client.appointments.create(
                patient={"phone": phone(100), "full_name": "Paciente Demo"},
                start_at=datetime(2026, 10, 5, 10, 30, tzinfo=timezone(timedelta(hours=-3))),
            )
            cancelled = await client.appointments.cancel(apt.id)
            assert cancelled.status == "cancelled"
            with pytest.raises(InvalidRequestError):
                await client.patients.retrieve("pat_noexiste")

        async with kuida.AsyncKuida(api_key=KEY_FLAKY_429, base_url=API_BASE, sleep=fake_sleep) as flaky:
            assert (await flaky.account.retrieve()).object == "account"
        assert sleeps == [1.0]

    asyncio.run(run())


def test_sync_iterator_rejects_async_page(mock):
    async def run():
        async with kuida.AsyncKuida(api_key=KEY_OK, base_url=API_BASE) as client:
            for i in range(3):
                await client.patients.create(phone=phone(200 + i))
            page = await client.patients.list(limit=1)
            it = page.auto_paging_iter()
            next(it)  # el primer elemento ya está en la página
            with pytest.raises(TypeError):
                next(it)

    asyncio.run(run())


# ─── parámetros ──────────────────────────────────────────────────────────────


def test_body_params_converts_only_known_keys():
    body = body_params(
        "AppointmentCreateParams",
        {
            "patient": {"phone": phone(1), "full_name": "Paciente Demo", "date_of_birth": date(1950, 1, 2)},
            "doctor": "doc_123",
            "start_at": datetime(2026, 10, 5, 13, 30, tzinfo=timezone.utc),
            "external_id": "T-1",
            "clave_desconocida": 1,
            "type": None,
        },
    )
    assert body == {
        "patient": {"phone": phone(1), "fullName": "Paciente Demo", "dateOfBirth": "1950-01-02"},
        "doctor": "doc_123",
        "startAt": "2026-10-05T13:30:00Z",
        "externalId": "T-1",
        "clave_desconocida": 1,
    }


def test_body_params_accepts_wire_keys_and_objects():
    patient = kuida.Patient.construct_from({"id": "pat_1", "object": "patient"})
    body = body_params("TreatmentCreateParams", {"patient": patient, "startDate": "2026-10-05T00:00:00-03:00", "name": "X"})
    assert body == {"patient": "pat_1", "startDate": "2026-10-05T00:00:00-03:00", "name": "X"}


def test_event_data_is_free():
    body = body_params(
        "EventInput",
        {"id": "e1", "type": "visit.completed", "schema_version": 1, "data": {"visit": {"visited_at": "x"}, "full_name": 1}},
    )
    assert body == {"id": "e1", "type": "visit.completed", "schemaVersion": 1, "data": {"visit": {"visited_at": "x"}, "full_name": 1}}


def test_naive_datetime_gets_local_zone():
    body = body_params("VisitCreateParams", {"patient": "pat_1", "visited_at": datetime(2026, 10, 5, 10, 30)})
    parsed = datetime.fromisoformat(body["visitedAt"].replace("Z", "+00:00"))
    assert parsed.tzinfo is not None


def test_query_params():
    q = query_params(
        kuida._generated.models.OPERATIONS["appointments.list"]["query"],
        {"start_at_gte": datetime(2026, 10, 1, tzinfo=timezone.utc), "starting_after": "apt_1", "limit": 5, "otro": True},
    )
    assert q == {"startAtGte": "2026-10-01T00:00:00Z", "startingAfter": "apt_1", "limit": 5, "otro": "true"}


# ─── modelos ─────────────────────────────────────────────────────────────────


def test_model_access_and_unknown_fields():
    raw = {
        "id": "pat_1",
        "object": "patient",
        "fullName": "Paciente Demo",
        "createdAt": "2026-09-28T12:00:00.123Z",
        "campoNuevo": {"a": 1},
    }
    p = kuida.Patient.construct_from(raw)
    assert p.full_name == p["fullName"] == "Paciente Demo"
    assert p.created_at == datetime(2026, 9, 28, 12, 0, 0, 123000, tzinfo=timezone.utc)
    assert p.email is None  # conocido pero ausente
    assert p.campo_nuevo == {"a": 1}  # desconocido: se conserva
    assert p.raw == raw and p.to_dict() == raw
    with pytest.raises(AttributeError):
        _ = p.no_existe
    with pytest.raises(AttributeError):
        p.full_name = "otro"
    assert pickle.loads(pickle.dumps(p)) == p


def test_missing_key_without_env(monkeypatch):
    monkeypatch.delenv("KUIDA_API_KEY", raising=False)
    with pytest.raises(kuida.AuthenticationError):
        kuida.AsyncKuida()


def test_env_configuration(monkeypatch):
    monkeypatch.setenv("KUIDA_API_KEY", KEY_OK)
    monkeypatch.setenv("KUIDA_API_BASE", "http://localhost:9/api/")
    c = kuida.Kuida()
    assert c.api_key == KEY_OK
    assert c.base_url == "http://localhost:9/api"
    assert c.api_version == kuida.API_VERSION == "2026-09-28"
    assert c.max_retries == 2 and c.timeout == 30
    c.close()

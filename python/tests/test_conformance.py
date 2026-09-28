"""Suite de conformidad del SDK de Python: los 17 escenarios de SDK_DESIGN.md §9."""

import json
import re
import time
import uuid
from datetime import datetime, timedelta, timezone

import pytest

import kuida
from kuida.errors import (
    APIConnectionError,
    APIError,
    AuthenticationError,
    IdempotencyError,
    InvalidRequestError,
    PermissionDeniedError,
    PermissionError as KuidaPermissionError,
    SignatureVerificationError,
)

from conftest import (
    KEY_EVENTS_ONLY,
    KEY_FLAKY_429,
    KEY_FLAKY_503,
    KEY_OK,
    KEY_REVOKED,
)

UUID4 = re.compile(r"^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$")
ART = timezone(timedelta(hours=-3))


def phone(n: int) -> str:
    return "+54 9 342 555 %04d" % n


# 1 ────────────────────────────────────────────────────────────────────────────
def test_account_retrieve(mock, client):
    account = client.account.retrieve()
    assert account.object == "account"
    assert isinstance(account, kuida.Account)
    assert "patients:write" in account.api_key.scopes
    assert account["apiKey"]["scopes"] == account.api_key.scopes
    assert account.last_response.status_code == 200
    assert account.last_response.request_id.startswith("req_")


# 2 ────────────────────────────────────────────────────────────────────────────
def test_headers(mock, client):
    client.account.retrieve()
    client.patients.create(phone=phone(1), full_name="Paciente Demo")
    client.patients.list(limit=1)

    reqs = mock.requests()
    assert [r["method"] for r in reqs] == ["GET", "POST", "GET"]
    for r in reqs:
        h = r["headers"]
        assert h["authorization"] == "Bearer " + KEY_OK
        assert h["kuida-version"] == "2026-09-28"
        assert re.match(r"^Kuida/v1 PythonBindings/\d+\.\d+\.\d+$", h["user-agent"])
        assert h["x-kuida-client-user-agent"]
        ua = json.loads(h["x-kuida-client-user-agent"])
        assert ua["lang"] == "python" and ua["bindings_version"] == kuida.VERSION
        assert {"lang_version", "platform"} <= set(ua)
        if r["method"] == "POST":
            assert UUID4.match(h["idempotency-key"])
            assert h["content-type"] == "application/json"
        else:
            assert h["idempotency-key"] is None


# 3 ────────────────────────────────────────────────────────────────────────────
def test_patients_crud(mock, client):
    created = client.patients.create(phone=phone(0), full_name="Paciente Demo", dni="11111111")
    assert created.id.startswith("pat_")
    assert created.full_name == "Paciente Demo"
    assert created["fullName"] == "Paciente Demo"
    assert isinstance(created.created_at, datetime) and created.created_at.tzinfo is not None
    assert mock.requests()[-1]["body"] == {"phone": phone(0), "fullName": "Paciente Demo", "dni": "11111111"}

    fetched = client.patients.retrieve(created.id)
    assert fetched.id == created.id

    updated = client.patients.update(created.id, email="paciente.demo@example.com")
    assert updated.email == "paciente.demo@example.com"

    page = client.patients.list(phone=phone(0))
    assert [p.id for p in page] == [created.id]
    assert page.has_more is False


# 4 ────────────────────────────────────────────────────────────────────────────
def test_idempotency_explicit(mock, client):
    key = str(uuid.uuid4())
    first = client.patients.create(phone=phone(10), full_name="Paciente Demo", idempotency_key=key)
    second = client.patients.create(phone=phone(10), full_name="Paciente Demo", idempotency_key=key)
    assert first.id == second.id
    assert second.last_response.idempotent_replayed is True

    with pytest.raises(IdempotencyError) as exc:
        client.patients.create(phone=phone(11), full_name="Paciente Otro", idempotency_key=key)
    assert exc.value.http_status == 400
    assert exc.value.code == "idempotency_key_reused"


# 5 ────────────────────────────────────────────────────────────────────────────
def test_pagination_manual(mock, client):
    for i in range(5):
        client.patients.create(phone=phone(20 + i), full_name="Paciente Demo %d" % i)

    first = client.patients.list(limit=2)
    assert len(first) == 2
    assert first.has_more is True

    second = client.patients.list(limit=2, starting_after=first.data[-1].id)
    assert len(second) == 2
    assert not {p.id for p in first} & {p.id for p in second}
    assert mock.requests()[-1]["query"] == {"limit": "2", "startingAfter": first[-1].id}


# 6 ────────────────────────────────────────────────────────────────────────────
def test_pagination_auto(mock, client):
    created = {client.patients.create(phone=phone(30 + i), full_name="Paciente Demo %d" % i).id for i in range(5)}

    seen = [p.id for p in client.patients.list(limit=2).auto_paging_iter()]
    assert len(seen) == len(set(seen)) == 5
    assert set(seen) == created

    lists = [r for r in mock.requests() if r["method"] == "GET" and r["path"] == "/v1/patients"]
    assert len(lists) == 3
    assert all(r["query"]["limit"] == "2" for r in lists)
    assert "startingAfter" not in lists[0]["query"]
    assert lists[1]["query"]["startingAfter"] == seen[1]
    assert lists[2]["query"]["startingAfter"] == seen[3]


# 7 ────────────────────────────────────────────────────────────────────────────
def test_appointments_flow(mock, client):
    start = datetime(2026, 10, 5, 10, 30, tzinfo=ART)
    apt = client.appointments.create(
        patient={"phone": phone(40), "full_name": "Paciente Demo"},
        doctor={"external_id": "MED-12", "full_name": "Dra. Demo"},
        start_at=start,
        external_id="turno-demo-1",
    )
    body = mock.requests()[-1]["body"]
    assert body["patient"] == {"phone": phone(40), "fullName": "Paciente Demo"}
    assert body["doctor"] == {"externalId": "MED-12", "fullName": "Dra. Demo"}
    assert body["startAt"] == "2026-10-05T10:30:00-03:00"
    assert apt.patient.startswith("pat_") and apt.doctor.startswith("doc_")
    assert apt.start_at == start
    assert apt.status == "confirmed"

    moved = client.appointments.update(apt.id, start_at="2026-10-06T11:00:00-03:00")
    assert moved.start_at == datetime(2026, 10, 6, 14, 0, tzinfo=timezone.utc)

    cancelled = client.appointments.cancel(apt.id)
    assert mock.requests()[-1]["body"] == {}
    assert cancelled.status == "cancelled"
    assert isinstance(cancelled.cancelled_at, datetime)

    with pytest.raises(InvalidRequestError) as exc:
        client.appointments.update(apt.id, start_at=start + timedelta(days=2))
    assert exc.value.http_status == 400


# 8 ────────────────────────────────────────────────────────────────────────────
def test_visit_closes_appointment(mock, client):
    apt = client.appointments.create(
        patient={"phone": phone(50), "full_name": "Paciente Demo"},
        start_at=datetime(2026, 10, 5, 10, 30, tzinfo=ART),
        external_id="turno-demo-50",
    )
    visit = client.visits.create(
        patient=apt.patient,
        visited_at=datetime(2026, 10, 5, 10, 45, tzinfo=ART),
        appointment="turno-demo-50",
        diagnosis="Control",
    )
    assert visit.id.startswith("vis_")
    assert visit.appointment == apt.id
    assert client.appointments.retrieve(apt.id).status == "completed"


# 9 ────────────────────────────────────────────────────────────────────────────
def test_intake_and_treatment(mock, client):
    patient = client.patients.create(phone=phone(60), full_name="Paciente Demo", dni="11111111")

    # En una solicitud de ingreso el paciente puede no existir todavía: va por identidad, no por id.
    intake = client.intake_requests.create(
        patient={"phone": phone(60), "full_name": "Paciente Demo", "dni": "11111111"},
        contacts=[{"name": "Familiar Demo", "relationship": "hija", "phone": phone(61)}],
        coverage={"insurer": "Obra Social Demo", "member_id": "0001"},
        requested_service="Internación domiciliaria",
        summary="Paciente Demo necesita seguimiento",
    )
    assert intake.id.startswith("int_")
    assert intake.patient == patient.id
    assert intake.status == "new"
    assert intake.data["coverage"] == {"insurer": "Obra Social Demo", "memberId": "0001"}
    assert intake.data["requestedService"] == "Internación domiciliaria"

    treatment = client.treatments.create(
        patient=patient.id,
        name="Enalapril 10 mg",
        dosage="1 comprimido",
        frequency="cada 12 horas",
        start_date=datetime(2026, 10, 5, tzinfo=ART),
    )
    assert treatment.id.startswith("trt_")
    assert treatment.patient == patient.id
    assert treatment.start_date == datetime(2026, 10, 5, 3, 0, tzinfo=timezone.utc)
    assert client.treatments.list(patient=patient.id, active=True).data[0].id == treatment.id
    assert mock.requests()[-1]["query"] == {"patient": patient.id, "active": "true"}


# 10 ───────────────────────────────────────────────────────────────────────────
def test_events_batch(mock, client):
    valid = {
        "id": "paciente-demo-alta",
        "type": "patient.upserted",
        "occurred_at": datetime(2026, 10, 5, 10, 0, tzinfo=ART),
        "data": {"phone": phone(70), "fullName": "Paciente Demo", "clave_libre": 1},
    }
    invalid = {"id": "evento-roto", "type": "no.existe", "data": {}}

    batch = client.events.create_batch([valid, invalid])
    assert batch.object == "event_batch"
    assert [r.status for r in batch.results] == ["processed", "invalid"]
    assert batch.results[0].accepted is True and batch.results[0].event.startswith("evt_")
    assert batch.results[0].result.object == "patient"

    sent = mock.requests()[-1]["body"]["events"][0]
    assert sent["occurredAt"] == "2026-10-05T10:00:00-03:00"
    assert sent["data"] == {"phone": phone(70), "fullName": "Paciente Demo", "clave_libre": 1}

    again = client.events.create(valid)
    assert again.results[0].status == "duplicate"

    # Si ningún evento entra: error normal y el detalle en el cuerpo crudo.
    with pytest.raises(InvalidRequestError) as exc:
        client.events.create_batch([invalid])
    assert exc.value.http_status == 400
    assert [r["status"] for r in exc.value.json_body["results"]] == ["invalid"]

    event = client.events.retrieve(batch.results[0].event)
    assert event.data == valid["data"]
    assert event.source_ref == "paciente-demo-alta"


# 11 ───────────────────────────────────────────────────────────────────────────
def test_webhook_endpoints_flow(mock, client):
    endpoint = client.webhook_endpoints.create(
        url="https://sistema.example/kuida/webhooks",
        enabled_events=["intake.ready", "intake.created"],
        description="Sistema de admisión",
    )
    assert endpoint.id.startswith("we_")
    assert endpoint.secret.startswith("whsec_")
    assert mock.requests()[-1]["body"]["enabledEvents"] == ["intake.ready", "intake.created"]

    fetched = client.webhook_endpoints.retrieve(endpoint.id)
    assert fetched.secret is None
    assert "secret" not in fetched

    updated = client.webhook_endpoints.update(endpoint.id, enabled_events=["*"], description="Todo")
    assert updated.enabled_events == ["*"]
    assert updated.description == "Todo"

    delivery = client.webhook_endpoints.ping(endpoint.id)
    assert mock.requests()[-1]["body"] == {}
    assert delivery.object == "webhook_delivery"
    assert delivery.status == "pending"
    assert delivery.event_type == "webhook.ping"
    assert delivery.payload["type"] == "webhook.ping"

    deliveries = client.webhook_deliveries.list(webhook_endpoint=endpoint.id)
    assert [d.id for d in deliveries] == [delivery.id]
    assert mock.requests()[-1]["query"] == {"webhookEndpoint": endpoint.id}
    assert client.webhook_deliveries.retrieve(delivery.id).id == delivery.id

    deleted = client.webhook_endpoints.delete(endpoint.id)
    assert deleted.deleted is True
    assert deleted.id == endpoint.id


# 12 ───────────────────────────────────────────────────────────────────────────
def test_errors(mock, make_client, monkeypatch):
    monkeypatch.delenv("KUIDA_API_KEY", raising=False)
    with pytest.raises(AuthenticationError) as exc:
        kuida.Kuida(base_url="http://localhost:1/api")
    assert exc.value.code == "api_key_missing"

    with pytest.raises(AuthenticationError) as exc:
        make_client("kd_test_invalida").account.retrieve()
    assert exc.value.http_status == 401
    assert exc.value.code == "api_key_invalid"

    with pytest.raises(AuthenticationError) as exc:
        make_client(KEY_REVOKED).account.retrieve()
    assert exc.value.code == "api_key_revoked"

    with pytest.raises(PermissionDeniedError) as exc:
        make_client(KEY_EVENTS_ONLY).patients.list()
    assert exc.value.http_status == 403
    assert exc.value.code == "scope_missing"
    assert isinstance(exc.value, KuidaPermissionError)

    client = make_client()
    with pytest.raises(InvalidRequestError) as exc:
        client.patients.retrieve("pat_noexiste")
    err = exc.value
    assert err.http_status == 404
    assert err.code == "resource_missing"
    assert err.request_id and err.request_id.startswith("req_")
    assert err.doc_url.endswith("#resource_missing")
    assert err.type == "invalid_request_error"

    with pytest.raises(InvalidRequestError) as exc:
        client.patients.create(full_name="Paciente Demo")
    assert exc.value.param == "phone"
    assert exc.value.code == "parameter_missing"
    assert isinstance(exc.value, kuida.KuidaError)


# 13 ───────────────────────────────────────────────────────────────────────────
def test_retries_5xx(mock, make_client, sleeps):
    client = make_client(KEY_FLAKY_503)
    patient = client.patients.create(phone=phone(80), full_name="Paciente Demo")
    assert patient.id.startswith("pat_")

    posts = [r for r in mock.requests() if r["method"] == "POST"]
    assert len(posts) == 2
    keys = {r["headers"]["idempotency-key"] for r in posts}
    assert len(keys) == 1 and UUID4.match(keys.pop())
    assert len(sleeps.calls) == 1
    assert 0.375 <= sleeps.calls[0] <= 0.625  # 0,5 s ± 25 %


# 14 ───────────────────────────────────────────────────────────────────────────
def test_retries_429(mock, make_client, sleeps):
    account = make_client(KEY_FLAKY_429).account.retrieve()
    assert account.object == "account"
    assert len(mock.requests()) == 2
    assert sleeps.calls == [1.0]  # Retry-After: 1


# 15 ───────────────────────────────────────────────────────────────────────────
def test_no_retry(mock, make_client, sleeps):
    client = make_client(KEY_FLAKY_503, max_retries=0)
    with pytest.raises(APIError) as exc:
        client.patients.create(phone=phone(90), full_name="Paciente Demo")
    assert exc.value.http_status == 503
    assert exc.value.code == "internal_error"
    assert len(mock.requests()) == 1
    assert sleeps.calls == []

    # También por pedido.
    with pytest.raises(APIError):
        make_client(KEY_FLAKY_503).patients.create(phone=phone(91), max_retries=0)


# 16 ───────────────────────────────────────────────────────────────────────────
def test_connection_error(make_client, sleeps):
    client = make_client(base_url="http://127.0.0.1:1/api", max_retries=0)
    with pytest.raises(APIConnectionError) as exc:
        client.account.retrieve()
    assert exc.value.http_status is None
    assert isinstance(exc.value, kuida.KuidaError)

    # Con reintentos, los agota antes de fallar.
    with pytest.raises(APIConnectionError):
        make_client(base_url="http://127.0.0.1:1/api", max_retries=2).account.retrieve()
    assert len(sleeps.calls) == 2


# 17 ───────────────────────────────────────────────────────────────────────────
def test_webhook_signature(vectors):
    v = vectors
    ts = int(v["timestamp"])

    assert kuida.Webhook.verify_signature(v["payload"], v["signatureHeader"], v["timestamp"], v["secret"], now=ts)
    assert kuida.Webhook.verify_signature(
        v["payload"].encode("utf-8"), v["signatureHeader"], v["timestamp"], v["secret"], tolerance=0
    )
    event = kuida.Webhook.construct_event(v["payload"], v["signatureHeader"], v["timestamp"], v["secret"], now=ts)
    assert isinstance(event, kuida.WebhookEvent)
    assert event.object == "event"
    assert event.type == "intake.ready"
    assert event.api_version == "2026-09-28"
    assert event.data["patient"] == {"fullName": "Paciente Demo"}

    bad_cases = [
        (v["payload"], v["signatureHeader"], v["timestamp"], v["wrongSecret"], {"now": ts}),
        (v["tamperedPayload"], v["signatureHeader"], v["timestamp"], v["secret"], {"now": ts}),
        (v["payload"], v["signatureHeader"], v["timestamp"], v["secret"], {}),  # fuera de tolerancia
        (v["payload"], v["signatureHeader"], v["timestamp"], v["secret"], {"now": ts + 301}),
        (v["payload"], None, v["timestamp"], v["secret"], {"now": ts}),
        (v["payload"], v["signatureHeader"], None, v["secret"], {"now": ts}),
    ]
    for payload, sig, stamp, secret, kw in bad_cases:
        with pytest.raises(SignatureVerificationError):
            kuida.Webhook.verify_signature(payload, sig, stamp, secret, **kw)
    with pytest.raises(SignatureVerificationError):
        kuida.Webhook.construct_event(v["tamperedPayload"], v["signatureHeader"], v["timestamp"], v["secret"], tolerance=0)

    # Cuerpo propio firmado con la hora actual: verifica con la tolerancia default.
    body = '{"id":"whd_demo","object":"event","type":"webhook.ping","apiVersion":"2026-09-28","livemode":false,"occurredAt":"2026-09-28T12:00:00.000Z","ref":"ping","data":{}}'
    now = str(int(time.time()))
    sig = kuida.Webhook.compute_signature(body, now, v["secret"])
    assert sig.startswith("sha256=")
    assert kuida.Webhook.construct_event(body, sig, now, v["secret"]).type == "webhook.ping"

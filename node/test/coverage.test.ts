/** Cada operación del OpenAPI tiene su método en el cliente (32 en total). */
import assert from "node:assert/strict";
import { test } from "node:test";
import { Kuida, OPERATIONS } from "../src/index.js";

test("operations_coverage", () => {
  const kuida = new Kuida("kd_test_000000000000_mocksecretmocksecret00", { baseUrl: "http://localhost:1/api" });
  assert.equal(OPERATIONS.length, 32);
  for (const op of OPERATIONS) {
    const resource = (kuida as unknown as Record<string, Record<string, unknown>>)[op.resource];
    assert.ok(resource, `falta el recurso ${op.resource}`);
    assert.equal(typeof resource[op.method], "function", `falta ${op.resource}.${op.method}`);
  }
  assert.equal(typeof kuida.events.createBatch, "function");
});

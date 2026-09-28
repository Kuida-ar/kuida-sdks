package ar.kuida;

/**
 * Objeto que sabe cómo viajar en el cuerpo JSON de un pedido.
 *
 * <p>Lo implementan los parámetros ({@link KuidaParams}) y las referencias id-o-identidad
 * ({@code PatientReference}, {@code DoctorReference}). El valor devuelto puede ser un texto,
 * un número, un booleano, un mapa, una lista u otro {@code JsonEncodable}.
 */
public interface JsonEncodable {
  /** Valor a serializar en lugar de este objeto. */
  Object toJsonValue();
}

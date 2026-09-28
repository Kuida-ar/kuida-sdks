package ar.kuida.service;

import ar.kuida.ApiRequestor;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/** Base de los servicios por recurso. */
abstract class ApiService {
  final ApiRequestor requestor;

  ApiService(ApiRequestor requestor) {
    this.requestor = requestor;
  }

  /** Codifica un id para usarlo como segmento de la ruta. */
  static String id(String id) {
    if (id == null || id.trim().isEmpty()) throw new IllegalArgumentException("El id no puede estar vacío");
    try {
      return URLEncoder.encode(id, "UTF-8").replace("+", "%20");
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException(e);
    }
  }
}

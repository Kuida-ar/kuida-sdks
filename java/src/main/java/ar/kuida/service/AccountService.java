package ar.kuida.service;

import ar.kuida.ApiRequestor;
import ar.kuida.RequestOptions;
import ar.kuida.model.Account;

/** {@code /v1/account}: la organización dueña de la clave. */
public final class AccountService extends ApiService {
  /** Uso interno: el servicio se obtiene desde {@link ar.kuida.KuidaClient}. */
  public AccountService(ApiRequestor requestor) {
    super(requestor);
  }

  /**
   * Obtiene la cuenta: la organización, sus líneas de servicio y los permisos de la clave. Sirve
   * para probar que la integración está bien configurada.
   */
  public Account retrieve() {
    return retrieve(null);
  }

  /** Igual que {@link #retrieve()}, con opciones por pedido. */
  public Account retrieve(RequestOptions options) {
    return requestor.request("GET", "/v1/account", null, Account.class, options);
  }
}

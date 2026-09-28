<?php

namespace Kuida\Service;

use Kuida\Model\Account;

/**
 * Tu organización y la clave de API en uso.
 */
class AccountService extends AbstractService
{
    /**
     * Devuelve la organización dueña de la clave, sus líneas de servicio y los
     * permisos (scopes) de la clave. Sirve para probar la conexión.
     *
     * @param array<string, mixed> $opts Opciones por pedido (`timeout`, `max_retries`).
     * @throws \Kuida\Exception\KuidaException
     */
    public function retrieve(array $opts = []): Account
    {
        return $this->requestObject('GET', '/v1/account', null, $opts, Account::class);
    }
}

<?php

namespace Kuida\Exception;

/**
 * La firma de un webhook no es válida: falta un header, la firma no coincide
 * o el timestamp está fuera de la tolerancia.
 */
class SignatureVerificationException extends KuidaException
{
    /** @var string|null */
    protected $sigHeader;

    /** @var string|null */
    protected $payload;

    /**
     * @param string $message Motivo del rechazo.
     * @param string|null $payload Cuerpo crudo recibido.
     * @param string|null $sigHeader Valor del header `x-kuida-signature`.
     */
    public static function factory(string $message, ?string $payload = null, ?string $sigHeader = null): self
    {
        $e = new self($message);
        $e->payload = $payload;
        $e->sigHeader = $sigHeader;

        return $e;
    }

    /**
     * Valor recibido del header `x-kuida-signature`.
     */
    public function getSigHeader(): ?string
    {
        return $this->sigHeader;
    }

    /**
     * Cuerpo crudo que se intentó verificar.
     */
    public function getPayload(): ?string
    {
        return $this->payload;
    }
}

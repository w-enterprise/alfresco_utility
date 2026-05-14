package com.welf.estraigruppiutenti.model;

/**
 * Eccezione per errori di connessione CMIS.
 */
public class CmisConnectionException extends Exception {
    /**
     * Crea un'eccezione con messaggio.
     *
     * @param message messaggio.
     */
    public CmisConnectionException(String message) {
        super(message);
    }

    /**
     * Crea un'eccezione con messaggio e causa.
     *
     * @param message messaggio.
     * @param cause   causa.
     */
    public CmisConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}

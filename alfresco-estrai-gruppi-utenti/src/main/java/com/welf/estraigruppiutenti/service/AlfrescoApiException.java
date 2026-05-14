package com.welf.estraigruppiutenti.service;

/**
 * Eccezione per errori durante l'invocazione delle API REST di Alfresco.
 */
public class AlfrescoApiException extends Exception {
    /**
     * Crea un'eccezione con messaggio.
     *
     * @param message messaggio.
     */
    public AlfrescoApiException(String message) {
        super(message);
    }

    /**
     * Crea un'eccezione con messaggio e causa.
     *
     * @param message messaggio.
     * @param cause   causa.
     */
    public AlfrescoApiException(String message, Throwable cause) {
        super(message, cause);
    }
}

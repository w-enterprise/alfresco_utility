package com.welf.estraigruppiutenti.model;

/**
 * Parametri di connessione ad Alfresco.
 */
public class ConnectionConfig {
    private final String url;
    private final String username;
    private final String password;

    /**
     * Crea una nuova configurazione.
     *
     * @param url      URL del servizio CMIS (AtomPub o Browser binding).
     * @param username Username.
     * @param password Password.
     */
    public ConnectionConfig(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    /**
     * @return URL del servizio CMIS.
     */
    public String getUrl() {
        return url;
    }

    /**
     * @return username.
     */
    public String getUsername() {
        return username;
    }

    /**
     * @return password in chiaro (solo in memoria).
     */
    public String getPassword() {
        return password;
    }
}

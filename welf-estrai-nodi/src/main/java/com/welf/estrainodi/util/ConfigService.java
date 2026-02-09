package com.welf.estrainodi.util;

import java.io.*;
import java.util.Properties;

/**
 * Gestisce il salvataggio e il caricamento della configurazione dell'applicazione.
 */
public class ConfigService {

    private static final String CONFIG_FILE = "alfresco_extractor_config.properties";
    
    // Chiavi per le proprietà
    public static final String KEY_URL = "cmis.url";
    public static final String KEY_USER = "cmis.user";
    public static final String KEY_PASS = "cmis.password";
    public static final String KEY_NODE_ID = "cmis.lastNodeId";
    public static final String KEY_MAX_ITEMS = "cmis.maxItems";

    private final File configFile;

    public ConfigService() {
        // Salva il file nella directory di esecuzione o user home
        // Qui usiamo la directory corrente per semplicità di accesso
        this.configFile = new File(CONFIG_FILE);
    }

    /**
     * Carica le proprietà dal file di configurazione.
     * @return Properties caricate o vuote se il file non esiste.
     */
    public Properties loadConfig() {
        Properties props = new Properties();
        if (configFile.exists()) {
            try (FileInputStream in = new FileInputStream(configFile)) {
                props.load(in);
            } catch (IOException e) {
                System.err.println("Impossibile caricare la configurazione: " + e.getMessage());
            }
        }
        return props;
    }

    /**
     * Salva le proprietà nel file di configurazione.
     * @param url L'URL di Alfresco.
     * @param user L'username.
     * @param pass La password.
     * @param nodeId L'ultimo Node ID utilizzato.
     * @param maxItems Il numero massimo di elementi da estrarre.
     */
    public void saveConfig(String url, String user, String pass, String nodeId, String maxItems) {
        Properties props = new Properties();
        props.setProperty(KEY_URL, url != null ? url : "");
        props.setProperty(KEY_USER, user != null ? user : "");
        props.setProperty(KEY_PASS, pass != null ? pass : ""); // Nota: salvataggio in chiaro come richiesto
        props.setProperty(KEY_NODE_ID, nodeId != null ? nodeId : "");
        props.setProperty(KEY_MAX_ITEMS, maxItems != null ? maxItems : "0");

        try (FileOutputStream out = new FileOutputStream(configFile)) {
            props.store(out, "Alfresco Extractor Configuration");
        } catch (IOException e) {
            System.err.println("Impossibile salvare la configurazione: " + e.getMessage());
        }
    }
}

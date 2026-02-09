package com.welf.estrainodi.model;

import org.apache.chemistry.opencmis.client.api.*;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.chemistry.opencmis.commons.exceptions.CmisConnectionException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Modello che gestisce la logica di business dell'applicazione:
 * connessione ad Alfresco tramite CMIS, recupero dei nodi e generazione del CSV.
 */
public class CmisModel {

    private Session session;

    /**
     * Classe interna per rappresentare le informazioni di un nodo.
     */
    public static class NodeInfo {
        private final String id;
        private final String name;

        public NodeInfo(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() { return id; }
        public String getName() { return name; }
    }

    /**
     * Stabilisce una connessione con il server Alfresco tramite CMIS.
     *
     * @param url      L'URL del servizio CMIS (AtomPub).
     * @param username Il nome utente per l'autenticazione.
     * @param password La password per l'autenticazione.
     * @throws CmisConnectionException Se la connessione fallisce.
     */
    public void connect(String url, String username, String password) throws CmisConnectionException {
        SessionFactory factory = SessionFactoryImpl.newInstance();
        Map<String, String> parameter = new HashMap<>();

        // Configurazione parametri di connessione
        parameter.put(SessionParameter.USER, username);
        parameter.put(SessionParameter.PASSWORD, password);
        parameter.put(SessionParameter.ATOMPUB_URL, url);
        parameter.put(SessionParameter.BINDING_TYPE, BindingType.ATOMPUB.value());
        
        try {
            List<Repository> repositories = factory.getRepositories(parameter);
            if (repositories != null && !repositories.isEmpty()) {
                this.session = repositories.get(0).createSession();
            } else {
                throw new CmisConnectionException("Nessun repository trovato all'URL specificato.");
            }
        } catch (Exception e) {
            throw new CmisConnectionException("Errore durante la connessione: " + e.getMessage(), e);
        }
    }

    /**
     * Verifica se la sessione è attiva.
     *
     * @return true se connesso, false altrimenti.
     */
    public boolean isConnected() {
        return this.session != null;
    }

    /**
     * Recupera i figli di una cartella specificata dal Node ID.
     *
     * @param folderNodeId Il Node ID della cartella di cui recuperare i contenuti.
     * @param maxItems Il numero massimo di nodi da recuperare (0 per nessun limite).
     * @return Una lista di NodeInfo contenente ID e Nome dei figli.
     * @throws CmisObjectNotFoundException Se il Node ID non corrisponde a un oggetto valido.
     * @throws IllegalArgumentException Se l'oggetto non è una cartella.
     */
    public List<NodeInfo> extractNodeIds(String folderNodeId, int maxItems) {
        if (!isConnected()) {
            throw new IllegalStateException("Non connesso al repository.");
        }

        CmisObject object = session.getObject(folderNodeId);
        
        if (!(object instanceof Folder)) {
            throw new IllegalArgumentException("Il Node ID specificato non corrisponde a una cartella.");
        }

        Folder folder = (Folder) object;
        List<NodeInfo> nodes = new ArrayList<>();
        
        OperationContext operationContext = session.createOperationContext();
        if (maxItems > 0) {
            operationContext.setMaxItemsPerPage(maxItems);
        }

        ItemIterable<CmisObject> children = folder.getChildren(operationContext);
        
        // Se maxItems > 0, limitiamo l'iterazione, altrimenti prendiamo tutto (OpenCMIS gestisce la paginazione)
        long count = 0;
        for (CmisObject child : children) {
            if (maxItems > 0 && count >= maxItems) {
                break;
            }
            nodes.add(new NodeInfo(child.getId(), child.getName()));
            count++;
        }

        return nodes;
    }

    /**
     * Genera un file CSV contenente i Node ID e i nomi estratti.
     * Il file viene salvato nella directory Downloads dell'utente.
     *
     * @param nodes La lista di nodi da scrivere nel CSV.
     * @return Il percorso assoluto del file generato.
     * @throws IOException Se si verifica un errore durante la scrittura del file.
     */
    public String generateCsv(List<NodeInfo> nodes) throws IOException {
        String userHome = System.getProperty("user.home");
        String downloadDir = userHome + File.separator + "Downloads";
        
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fileName = "alfresco_contents_" + timestamp + ".csv";
        File file = new File(downloadDir, fileName);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            // Intestazione
            writer.write("Node ID,Name");
            writer.newLine();

            // Dati
            for (NodeInfo node : nodes) {
                String safeName = escapeCsv(node.getName());
                writer.write(node.getId() + "," + safeName);
                writer.newLine();
            }
        }

        return file.getAbsolutePath();
    }

    /**
     * Esegue l'escape di una stringa per il formato CSV.
     * Se contiene virgole o virgolette, racchiude in virgolette e raddoppia quelle interne.
     */
    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}

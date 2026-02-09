package com.welf.estrainodi.controller;

import com.welf.estrainodi.model.CmisModel;
import com.welf.estrainodi.util.ConfigService;
import com.welf.estrainodi.view.AppView;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

/**
 * Controller che gestisce l'interazione tra View e Model.
 * Utilizza SwingWorker per eseguire le operazioni di rete in background.
 */
public class AppController {

    private final AppView view;
    private final CmisModel model;
    private final ConfigService configService;

    /**
     * Costruttore del Controller.
     *
     * @param view  L'istanza della vista.
     * @param model L'istanza del modello.
     */
    public AppController(AppView view, CmisModel model) {
        this.view = view;
        this.model = model;
        this.configService = new ConfigService();

        // Inizializza la view con i dati salvati (se presenti)
        loadSavedConfiguration();

        // Associa il listener al pulsante
        this.view.setExtractButtonListener(new ExtractAction());
    }

    private void loadSavedConfiguration() {
        Properties props = configService.loadConfig();
        if (!props.isEmpty()) {
            String url = props.getProperty(ConfigService.KEY_URL);
            String user = props.getProperty(ConfigService.KEY_USER);
            String pass = props.getProperty(ConfigService.KEY_PASS);
            String nodeId = props.getProperty(ConfigService.KEY_NODE_ID);
            String maxItems = props.getProperty(ConfigService.KEY_MAX_ITEMS);

            if (url != null && !url.isEmpty()) view.setCmisUrl(url);
            if (user != null && !user.isEmpty()) view.setUsername(user);
            if (pass != null && !pass.isEmpty()) view.setPassword(pass);
            if (nodeId != null && !nodeId.isEmpty()) view.setNodeId(nodeId);
            if (maxItems != null && !maxItems.isEmpty()) {
                try {
                    view.setMaxItems(Integer.parseInt(maxItems));
                } catch (NumberFormatException e) {
                    view.setMaxItems(0);
                }
            }
        }
    }

    /**
     * Gestore dell'azione del pulsante "Estrai".
     */
    private class ExtractAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String nodeId = view.getNodeId();
            String url = view.getCmisUrl();
            String user = view.getUsername();
            String pass = view.getPassword();
            int maxItems = view.getMaxItems();

            if (nodeId == null || nodeId.trim().isEmpty()) {
                view.showError("Errore Input", "Inserire un Node ID valido.");
                return;
            }

            // Disabilita controlli e avvia progress bar
            view.setControlsEnabled(false);
            view.setProgressIndeterminate(true);
            view.appendLog("--- Inizio Elaborazione ---");
            view.appendLog("Connessione a: " + url);
            view.appendLog("Max items richiesti: " + (maxItems == 0 ? "Illimitati" : maxItems));

            // Avvia il task in background
            new ExtractionWorker(url, user, pass, nodeId, maxItems).execute();
        }
    }

    /**
     * Worker per eseguire le operazioni pesanti in background senza bloccare la UI.
     */
    private class ExtractionWorker extends SwingWorker<String, String> {
        private final String url;
        private final String user;
        private final String pass;
        private final String nodeId;
        private final int maxItems;
        private long startTime;

        public ExtractionWorker(String url, String user, String pass, String nodeId, int maxItems) {
            this.url = url;
            this.user = user;
            this.pass = pass;
            this.nodeId = nodeId;
            this.maxItems = maxItems;
        }

        @Override
        protected String doInBackground() throws Exception {
            this.startTime = System.currentTimeMillis();
            publish("Tentativo di connessione...");
            model.connect(url, user, pass);
            publish("Connesso con successo.");

            publish("Recupero contenuti per Node ID: " + nodeId);
            List<CmisModel.NodeInfo> nodes = model.extractNodeIds(nodeId, maxItems);
            
            for (CmisModel.NodeInfo node : nodes) {
                publish("Trovato nodo: " + node.getName() + " [" + node.getId() + "]");
            }
            
            publish("Totale elementi trovati: " + nodes.size());

            if (nodes.isEmpty()) {
                publish("Cartella vuota. Nessun file da generare.");
                return null;
            }

            publish("Generazione file CSV...");
            String filePath = model.generateCsv(nodes);
            return filePath;
        }

        @Override
        protected void process(List<String> chunks) {
            for (String message : chunks) {
                view.appendLog(message);
            }
        }

        @Override
        protected void done() {
            try {
                String resultPath = get();
                if (resultPath != null) {
                    view.appendLog("File salvato in: " + resultPath);
                    
                    // Salva la configurazione dopo un successo
                    configService.saveConfig(url, user, pass, nodeId, String.valueOf(maxItems));
                    view.appendLog("Parametri di configurazione salvati.");

                    view.showSuccess("Operazione Completata", "File CSV generato con successo:\n" + resultPath);
                } else {
                    view.appendLog("Operazione conclusa senza generazione file (cartella vuota).");
                }
            } catch (InterruptedException ex) {
                view.appendLog("Operazione interrotta.");
            } catch (ExecutionException ex) {
                Throwable cause = ex.getCause();
                view.appendLog("ERRORE: " + cause.getMessage());
                view.showError("Errore Elaborazione", "Si è verificato un errore:\n" + cause.getMessage());
                cause.printStackTrace();
            } finally {
                view.setControlsEnabled(true);
                view.stopProgress();
                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;
                view.appendLog("Tempo di elaborazione: " + duration + " ms (" + (duration / 1000.0) + " s)");
                view.appendLog("--- Fine Elaborazione ---");
            }
        }
    }
}

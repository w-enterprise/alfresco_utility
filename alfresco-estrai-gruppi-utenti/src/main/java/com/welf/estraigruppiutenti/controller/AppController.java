package com.welf.estraigruppiutenti.controller;

import com.welf.estraigruppiutenti.model.CmisConnectionException;
import com.welf.estraigruppiutenti.model.ConnectionConfig;
import com.welf.estraigruppiutenti.model.GroupInfo;
import com.welf.estraigruppiutenti.service.AlfrescoApiException;
import com.welf.estraigruppiutenti.service.AlfrescoGroupsService;
import com.welf.estraigruppiutenti.service.CmisConnectionService;
import com.welf.estraigruppiutenti.util.AlfrescoUrlUtils;
import com.welf.estraigruppiutenti.util.ConfigService;
import com.welf.estraigruppiutenti.util.CsvUtils;
import com.welf.estraigruppiutenti.view.MainFrame;
import org.apache.chemistry.opencmis.client.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingWorker;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Controller principale: collega vista e servizi.
 */
public class AppController {
    private static final Logger logger = LoggerFactory.getLogger(AppController.class);
    private static final DateTimeFormatter EXPORT_TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final MainFrame frame;
    private final ConfigService configService;
    private final CmisConnectionService cmisConnectionService;

    private Session cmisSession;
    private AlfrescoGroupsService groupsService;

    private SwingWorker<Void, Void> connectWorker;
    private SwingWorker<List<GroupInfo>, Void> loadGroupsWorker;
    private SwingWorker<Path, Void> exportWorker;

    /**
     * Crea un controller associato a una finestra.
     *
     * @param frame finestra principale.
     */
    public AppController(MainFrame frame) {
        this.frame = frame;
        this.configService = new ConfigService();
        this.cmisConnectionService = new CmisConnectionService();
        wireUiActions();
    }

    /**
     * Avvia l'applicazione: mostra la form principale e permette la connessione.
     */
    public void start() {
        ensureLogsDir();
        ConnectionConfig initial = configService.load();
        frame.setConnectionConfig(initial);
        frame.setControlsEnabled(false);
        frame.setVisible(true);
    }

    /**
     * Connette CMIS e inizializza i servizi Alfresco in background.
     *
     * @param config configurazione.
     */
    public void connectAsync(ConnectionConfig config) {
        if (connectWorker != null && !connectWorker.isDone()) {
            frame.showError("Connessione", "Connessione già in corso.");
            return;
        }
        if (exportWorker != null && !exportWorker.isDone()) {
            frame.showError("Connessione", "Interrompi prima l'esportazione in corso.");
            return;
        }

        frame.setProgressVisible(true);
        frame.setIndeterminateProgress("Connessione CMIS...");
        frame.setStatus("Connessione in corso...");
        frame.setControlsEnabled(false);
        frame.setExportEnabled(false);
        frame.setConnectionControlsEnabled(false);

        connectWorker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    cmisSession = cmisConnectionService.createSession(config.getUrl(), config.getUsername(), config.getPassword());
                    logger.info("Connessione CMIS OK. Repository: {}", cmisSession.getRepositoryInfo().getName());

                    String baseUrl = AlfrescoUrlUtils.toAlfrescoBaseUrl(config.getUrl());
                    if (baseUrl.isBlank()) {
                        throw new CmisConnectionException("Impossibile derivare l'URL base Alfresco dall'URL CMIS.");
                    }
                    groupsService = new AlfrescoGroupsService(baseUrl, config.getUsername(), config.getPassword());
                    configService.save(config);
                    return null;
                } catch (Exception e) {
                    logger.debug("Connessione fallita", e);
                    throw e;
                }
            }

            @Override
            protected void done() {
                try {
                    get();
                    frame.setStatus("Connesso. Caricamento gruppi...");
                    loadGroupsAsync();
                } catch (Exception e) {
                    groupsService = null;
                    cmisSession = null;
                    String msg = safeMessage(e.getCause() == null ? e : e.getCause());
                    frame.showError("Connessione fallita", msg);
                    logger.info("Connessione fallita: {}", msg);
                    frame.setStatus("Connessione fallita.");
                } finally {
                    frame.setProgressVisible(false);
                    frame.setIndeterminateProgress(" ");
                    frame.setConnectionControlsEnabled(true);
                }
            }
        };
        connectWorker.execute();
    }

    /**
     * Avvia il caricamento dei gruppi in background.
     */
    public void loadGroupsAsync() {
        if (groupsService == null) {
            frame.showError("Errore", "Connessione non inizializzata.");
            return;
        }
        if (loadGroupsWorker != null && !loadGroupsWorker.isDone()) {
            loadGroupsWorker.cancel(true);
        }

        frame.setProgressVisible(true);
        frame.setIndeterminateProgress("Caricamento gruppi...");
        frame.setStatus("Recupero gruppi dal repository...");
        frame.setControlsEnabled(false);
        frame.setExportEnabled(false);

        loadGroupsWorker = new SwingWorker<>() {
            @Override
            protected List<GroupInfo> doInBackground() throws Exception {
                logger.info("Inizio caricamento gruppi...");
                return groupsService.fetchAllGroups(this::isCancelled);
            }

            @Override
            protected void done() {
                try {
                    List<GroupInfo> groups = get();
                    frame.setGroups(groups);
                    frame.setExportEnabled(!groups.isEmpty());
                    frame.setStatus("Gruppi caricati: " + groups.size());
                    logger.info("Gruppi caricati: {}", groups.size());
                } catch (Exception e) {
                    String msg = safeMessage(e.getCause() == null ? e : e.getCause());
                    frame.showError("Errore caricamento gruppi", msg);
                    logger.info("Errore caricamento gruppi: {}", msg);
                } finally {
                    frame.setProgressVisible(false);
                    frame.setControlsEnabled(true);
                }
            }
        };
        loadGroupsWorker.execute();
    }

    /**
     * Avvia l'esportazione CSV in background.
     */
    public void exportSelectedAsync() {
        if (groupsService == null) {
            frame.showError("Errore", "Connessione non inizializzata.");
            return;
        }
        List<GroupInfo> selected = frame.getSelectedGroups();
        if (selected.isEmpty()) {
            frame.showError("Esportazione", "Seleziona almeno un gruppo.");
            return;
        }

        if (exportWorker != null && !exportWorker.isDone()) {
            frame.showError("Esportazione", "Esportazione già in corso.");
            return;
        }

        frame.setProgressVisible(true);
        frame.setIndeterminateProgress("Esportazione...");
        frame.setStatus("Esportazione in corso...");
        frame.setExporting(true);

        exportWorker = new SwingWorker<>() {
            private Path outputFile;

            @Override
            protected Path doInBackground() throws Exception {
                String ts = LocalDateTime.now().format(EXPORT_TS);
                outputFile = Paths.get("gruppi_utenti_alfresco_" + ts + ".csv").toAbsolutePath();

                Set<String> exportedUsers = new HashSet<>();
                long rows = 0;

                try (var writer = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8)) {
                    // Intestazione CSV
                    writer.write("gruppo;utente;ripetuto");
                    writer.write(System.lineSeparator());

                    for (GroupInfo group : selected) {
                        if (isCancelled()) {
                            throw new AlfrescoApiException("Operazione cancellata.");
                        }
                        String groupName = group.getDisplayName();
                        logger.info("Esporto gruppo: {} (utenti diretti: {})", groupName, group.getDirectUsersCount());
                        List<String> users = groupsService.fetchGroupUserMembers(group.getId(), this::isCancelled);
                        for (String username : users) {
                            if (isCancelled()) {
                                throw new AlfrescoApiException("Operazione cancellata.");
                            }
                            boolean already = !exportedUsers.add(username);
                            String marker = already ? "*" : "";
                            writer.write(CsvUtils.escape(groupName));
                            writer.write(";");
                            writer.write(CsvUtils.escape(username));
                            writer.write(";");
                            writer.write(marker);
                            writer.write(System.lineSeparator());
                            rows++;
                        }
                    }
                }

                logger.info("Esportazione completata. Righe: {}. File: {}", rows, outputFile);
                return outputFile;
            }

            @Override
            protected void done() {
                try {
                    Path file = get();
                    frame.setStatus("Esportazione completata: " + file);
                } catch (Exception e) {
                    if (isCancelled()) {
                        if (outputFile != null) {
                            try {
                                Files.deleteIfExists(outputFile);
                            } catch (Exception ignored) {
                            }
                        }
                        frame.setStatus("Esportazione interrotta.");
                        logger.info("Esportazione interrotta.");
                    } else {
                        String msg = safeMessage(e.getCause() == null ? e : e.getCause());
                        frame.showError("Errore esportazione", msg);
                        logger.info("Errore esportazione: {}", msg);
                    }
                } finally {
                    frame.setProgressVisible(false);
                    frame.setExporting(false);
                }
            }
        };
        exportWorker.execute();
    }

    /**
     * Interrompe l'export in corso, se presente.
     */
    public void cancelExport() {
        if (exportWorker != null && !exportWorker.isDone()) {
            exportWorker.cancel(true);
        }
    }

    private void wireUiActions() {
        frame.getConnectButton().addActionListener(e -> connectAsync(frame.getConnectionConfig()));
        frame.getRefreshButton().addActionListener(e -> loadGroupsAsync());
        frame.getExportButton().addActionListener(e -> exportSelectedAsync());
        frame.getStopButton().addActionListener(e -> cancelExport());
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                shutdown();
                frame.dispose();
                System.exit(0);
            }
        });
    }

    /**
     * Rilascia le risorse e interrompe eventuali operazioni in corso.
     */
    public void shutdown() {
        try {
            if (connectWorker != null && !connectWorker.isDone()) {
                connectWorker.cancel(true);
            }
            if (loadGroupsWorker != null && !loadGroupsWorker.isDone()) {
                loadGroupsWorker.cancel(true);
            }
            if (exportWorker != null && !exportWorker.isDone()) {
                exportWorker.cancel(true);
            }
            if (cmisSession != null) {
                try {
                    cmisSession.getBinding().clearAllCaches();
                } catch (Exception ignored) {
                }
            }
        } finally {
            cmisSession = null;
            groupsService = null;
        }
    }

    private static String safeMessage(Throwable t) {
        String msg = t.getMessage();
        return msg == null ? t.getClass().getSimpleName() : msg;
    }

    private static void ensureLogsDir() {
        try {
            Files.createDirectories(Paths.get("logs"));
        } catch (Exception ignored) {
        }
    }
}

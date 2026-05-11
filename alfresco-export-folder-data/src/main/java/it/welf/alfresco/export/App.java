package it.welf.alfresco.export;

import com.formdev.flatlaf.FlatLightLaf;
import it.welf.alfresco.export.model.ConfigManager;
import it.welf.alfresco.export.model.NodeInfo;
import it.welf.alfresco.export.service.CmisService;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

public class App extends JFrame {
    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

    private static final Pattern INVALID_WINDOWS_CHARS = Pattern.compile("[\\\\/:*?\"<>|]");

    private JTextField addressField;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton connectButton;
    private JTree nodeTree;
    private DefaultTreeModel treeModel;
    private DefaultMutableTreeNode rootNode;
    private CmisService cmisService;
    private ConfigManager configManager;
    private JLabel statusLabel;
    
    // Nuovi campi per l'esportazione
    private JSpinner depthSpinner;
    private JTextField csvNameField;
    private JButton exportButton;
    private JButton cancelExportButton;
    private JTextArea logArea;
    private JLabel selectedFolderNameLabel;
    private JLabel selectedNodeIdLabel;
    private JButton copyButton;
    private JTextField startNodePathField;
    private JButton validatePathButton;
    private NodeInfo manualStartNode;

    private SwingWorker<Boolean, String> exportWorker;
    private final AtomicBoolean exportCancelRequested = new AtomicBoolean(false);

    private boolean csvNameDirty = false;
    private boolean csvNameProgrammaticUpdate = false;

    private static final String CMIS_PATH = "/alfresco/api/-default-/public/cmis/versions/1.1/atom";

    public App() {
        setTitle("Alfresco Node Explorer & Permission Exporter");
        setSize(1000, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        cmisService = new CmisService();
        configManager = new ConfigManager();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                LOGGER.info("Avvio applicazione");
            }

            @Override
            public void windowClosing(WindowEvent e) {
                LOGGER.info("Chiusura applicazione richiesta");
            }
        });

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        setContentPane(mainPanel);

        // --- Pannello Superiore: Connessione ---
        JPanel topPanel = new JPanel(new GridBagLayout());
        topPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Connessione Alfresco"),
                new EmptyBorder(10, 10, 10, 10)));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        topPanel.add(new JLabel("Indirizzo (IP:Porta):"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        addressField = new JTextField("localhost:8080");
        topPanel.add(addressField, gbc);

        gbc.gridx = 2; gbc.gridy = 0; gbc.weightx = 0;
        topPanel.add(new JLabel("Utente:"), gbc);
        gbc.gridx = 3; gbc.gridy = 0; gbc.weightx = 1.0;
        usernameField = new JTextField("admin");
        topPanel.add(usernameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        topPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0;
        passwordField = new JPasswordField("admin");
        topPanel.add(passwordField, gbc);

        gbc.gridx = 2; gbc.gridy = 1; gbc.gridwidth = 2;
        connectButton = new JButton("Connetti ed Esplora");
        connectButton.setBackground(new Color(0, 120, 215));
        connectButton.setForeground(Color.WHITE);
        topPanel.add(connectButton, gbc);

        mainPanel.add(topPanel, BorderLayout.NORTH);

        // --- Pannello Centrale: Split Tree e Log ---
        rootNode = new DefaultMutableTreeNode("Non connesso");
        treeModel = new DefaultTreeModel(rootNode);
        nodeTree = new JTree(treeModel);
        nodeTree.setRowHeight(25);
        nodeTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        
        JScrollPane treeScroll = new JScrollPane(nodeTree);
        treeScroll.setBorder(BorderFactory.createTitledBorder("Esplora e Seleziona un Nodo"));

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Log Operazioni"));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, treeScroll, logScroll);
        splitPane.setDividerLocation(400);
        mainPanel.add(splitPane, BorderLayout.CENTER);

        // --- Pannello Destro: Configurazione Esportazione ---
        JPanel rightPanel = new JPanel(new GridBagLayout());
        rightPanel.setPreferredSize(new Dimension(300, 0));
        rightPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Opzioni Esportazione"),
                new EmptyBorder(10, 10, 10, 10)));

        GridBagConstraints rGbc = new GridBagConstraints();
        rGbc.insets = new Insets(5, 5, 5, 5);
        rGbc.fill = GridBagConstraints.HORIZONTAL;
        rGbc.gridx = 0; rGbc.gridy = 0;

        // Info Nodo Selezionato
        rightPanel.add(new JLabel("Cartella Selezionata:"), rGbc);
        rGbc.gridy = 1;
        selectedFolderNameLabel = new JLabel("-");
        selectedFolderNameLabel.setForeground(new Color(0, 120, 215));
        selectedFolderNameLabel.setFont(selectedFolderNameLabel.getFont().deriveFont(Font.BOLD));
        rightPanel.add(selectedFolderNameLabel, rGbc);

        rGbc.gridy = 2;
        rightPanel.add(new JLabel("Node-ID:"), rGbc);
        
        rGbc.gridy = 3;
        JPanel idPanel = new JPanel(new BorderLayout(5, 0));
        selectedNodeIdLabel = new JLabel("-");
        selectedNodeIdLabel.setFont(selectedNodeIdLabel.getFont().deriveFont(11f));
        idPanel.add(selectedNodeIdLabel, BorderLayout.CENTER);
        
        copyButton = new JButton("Copia");
        copyButton.setMargin(new Insets(2, 5, 2, 5));
        copyButton.setEnabled(false);
        idPanel.add(copyButton, BorderLayout.EAST);
        rightPanel.add(idPanel, rGbc);

        rGbc.gridy = 4;
        rGbc.insets = new Insets(15, 5, 5, 5);
        rightPanel.add(new JSeparator(), rGbc);

        rGbc.gridy = 5;
        rGbc.insets = new Insets(5, 5, 5, 5);
        rightPanel.add(new JLabel("Nodo di partenza (path o nodeId):"), rGbc);

        rGbc.gridy = 6;
        JPanel pathPanel = new JPanel(new BorderLayout(5, 0));
        startNodePathField = new JTextField("");
        pathPanel.add(startNodePathField, BorderLayout.CENTER);
        validatePathButton = new JButton("Valida");
        validatePathButton.setEnabled(false);
        pathPanel.add(validatePathButton, BorderLayout.EAST);
        rightPanel.add(pathPanel, rGbc);

        rGbc.gridy = 7;
        rGbc.insets = new Insets(15, 5, 5, 5);
        rightPanel.add(new JSeparator(), rGbc);

        rGbc.gridy = 8;
        rGbc.insets = new Insets(5, 5, 5, 5);
        rightPanel.add(new JLabel("Profondità (0=tutti):"), rGbc);
        rGbc.gridy = 9;
        depthSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
        rightPanel.add(depthSpinner, rGbc);

        rGbc.gridy = 10;
        rightPanel.add(new JLabel("Nome File CSV:"), rGbc);
        rGbc.gridy = 11;
        String defaultFileName = "export_permissions_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";
        csvNameField = new JTextField(defaultFileName);
        rightPanel.add(csvNameField, rGbc);
        csvNameField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                onCsvNameFieldChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                onCsvNameFieldChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                onCsvNameFieldChanged();
            }
        });

        rGbc.gridy = 12;
        rGbc.weighty = 0.0;
        rGbc.anchor = GridBagConstraints.NORTH;
        rGbc.insets = new Insets(20, 5, 5, 5);
        exportButton = new JButton("Avvia Esportazione");
        exportButton.setEnabled(false);
        exportButton.setFont(exportButton.getFont().deriveFont(Font.BOLD));
        rightPanel.add(exportButton, rGbc);

        rGbc.gridy = 13;
        rGbc.insets = new Insets(5, 5, 5, 5);
        cancelExportButton = new JButton("Interrompi Esportazione");
        cancelExportButton.setEnabled(false);
        cancelExportButton.setBackground(new Color(220, 53, 69));
        cancelExportButton.setForeground(Color.WHITE);
        rightPanel.add(cancelExportButton, rGbc);

        rGbc.gridy = 14;
        rGbc.weighty = 1.0;
        rightPanel.add(new JPanel(), rGbc);

        mainPanel.add(rightPanel, BorderLayout.EAST);

        // --- Barra di Stato ---
        statusLabel = new JLabel("Pronto.");
        statusLabel.setBorder(new EmptyBorder(5, 5, 5, 5));
        mainPanel.add(statusLabel, BorderLayout.SOUTH);

        // --- Azioni ---
        connectButton.addActionListener(e -> connectAndLoadNodes());
        
        nodeTree.addTreeSelectionListener(e -> {
            TreePath path = nodeTree.getSelectionPath();
            if (path != null) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                if (node.getUserObject() instanceof NodeInfo) {
                    NodeInfo ni = (NodeInfo) node.getUserObject();
                    manualStartNode = null;
                    selectedFolderNameLabel.setText(ni.getName());
                    selectedNodeIdLabel.setText(ni.getId());
                    copyButton.setEnabled(true);
                    exportButton.setEnabled(cmisService.getSession() != null);
                    if (ni.getPath() != null && !ni.getPath().trim().isEmpty()) {
                        startNodePathField.setText(ni.getPath());
                    }
                    updateCsvNameFieldAuto(ni.getName());
                } else {
                    resetSelectionLabels();
                }
            } else {
                resetSelectionLabels();
            }
        });

        copyButton.addActionListener(e -> {
            String id = selectedNodeIdLabel.getText();
            if (id != null && !id.equals("-")) {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(id), null);
                statusLabel.setText("Node-ID copiato negli appunti.");
            }
        });

        exportButton.addActionListener(e -> startExport());
        cancelExportButton.addActionListener(e -> requestCancelExport());
        validatePathButton.addActionListener(e -> validateAndUseStartPath());

        loadSavedConfig();
    }

    private void onCsvNameFieldChanged() {
        if (csvNameProgrammaticUpdate) {
            return;
        }
        if (!csvNameDirty) {
            csvNameDirty = true;
            LOGGER.info("Campo nome CSV modificato manualmente dall'utente");
        }
    }

    private void updateCsvNameFieldAuto(String folderName) {
        if (csvNameDirty) {
            return;
        }
        String nodeId = (manualStartNode != null) ? manualStartNode.getId() : selectedNodeIdLabel.getText();
        int depth = (Integer) depthSpinner.getValue();
        String autoName = buildAutoCsvFileName(folderName, nodeId, depth);
        setCsvNameFieldValue(autoName);
    }

    private String buildAutoCsvFileName(String folderName, String nodeId, int depth) {
        return ExportFileNameUtils.buildExportFileName(folderName, LocalDateTime.now(), nodeId, depth);
    }

    private void setCsvNameFieldValue(String value) {
        csvNameProgrammaticUpdate = true;
        try {
            csvNameField.setText(value);
        } finally {
            csvNameProgrammaticUpdate = false;
        }
    }

    private void resetSelectionLabels() {
        selectedFolderNameLabel.setText("-");
        selectedNodeIdLabel.setText("-");
        copyButton.setEnabled(false);
        exportButton.setEnabled(false);
        cancelExportButton.setEnabled(false);
        manualStartNode = null;
    }

    private void loadSavedConfig() {
        ConfigManager.ConnectionConfig config = configManager.loadConfig();
        if (!config.getAddress().isEmpty()) {
            addressField.setText(config.getAddress());
            usernameField.setText(config.getUsername());
            passwordField.setText(config.getPassword());
        }
    }

    private void connectAndLoadNodes() {
        String address = addressField.getText().trim();
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        String protocol = address.startsWith("http") ? "" : "http://";
        String fullUrl = protocol + address + CMIS_PATH;

        connectButton.setEnabled(false);
        statusLabel.setText("Connessione in corso...");
        logArea.setText("");
        LOGGER.info("Connessione richiesta: address={}, username={}", address, username);

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                cmisService.connect(fullUrl, username, password);
                configManager.saveConfig(address, username, password);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    statusLabel.setText("Connesso. Caricamento nodi...");
                    LOGGER.info("Connessione completata: {}", fullUrl);
                    validatePathButton.setEnabled(true);
                    loadRootNodes();
                } catch (Exception ex) {
                    statusLabel.setText("Errore connessione.");
                    LOGGER.error("Errore connessione: {}", fullUrl, ex);
                    JOptionPane.showMessageDialog(App.this, "Errore: " + ex.getMessage(), "Errore", JOptionPane.ERROR_MESSAGE);
                    connectButton.setEnabled(true);
                    validatePathButton.setEnabled(false);
                }
            }
        };
        worker.execute();
    }

    private void validateAndUseStartPath() {
        if (cmisService.getSession() == null) {
            JOptionPane.showMessageDialog(this, "Connettersi ad Alfresco prima di validare il nodo.", "Non connesso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String input = startNodePathField.getText();
        if (input == null || input.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Inserire un percorso Alfresco (es. /Sites/.../documentLibrary) oppure un nodeId.", "Validazione", JOptionPane.WARNING_MESSAGE);
            return;
        }

        statusLabel.setText("Validazione nodo in corso...");
        LOGGER.info("Validazione nodo da input: {}", input);

        validatePathButton.setEnabled(false);
        exportButton.setEnabled(false);

        SwingWorker<NodeInfo, Void> worker = new SwingWorker<NodeInfo, Void>() {
            @Override
            protected NodeInfo doInBackground() {
                String trimmed = input.trim();
                Folder folder = trimmed.startsWith("/") ? cmisService.getFolderByPath(trimmed) : cmisService.getFolderById(trimmed);
                String folderPath = "";
                if (folder instanceof org.apache.chemistry.opencmis.client.api.FileableCmisObject) {
                    List<String> paths = ((org.apache.chemistry.opencmis.client.api.FileableCmisObject) folder).getPaths();
                    if (paths != null && !paths.isEmpty()) {
                        folderPath = paths.get(0);
                    }
                }
                return new NodeInfo(folder.getId(), folder.getName(), folder.getType().getId(), folderPath, true);
            }

            @Override
            protected void done() {
                try {
                    NodeInfo ni = get();
                    manualStartNode = ni;
                    selectedFolderNameLabel.setText(ni.getName());
                    selectedNodeIdLabel.setText(ni.getId());
                    copyButton.setEnabled(true);
                    exportButton.setEnabled(true);
                    statusLabel.setText("Nodo valido selezionato.");
                    if (ni.getPath() != null && !ni.getPath().trim().isEmpty()) {
                        startNodePathField.setText(ni.getPath());
                    }
                    updateCsvNameFieldAuto(ni.getName());
                    LOGGER.info("Nodo valido: id={}, name={}, path={}", ni.getId(), ni.getName(), ni.getPath());
                } catch (Exception ex) {
                    manualStartNode = null;
                    exportButton.setEnabled(false);
                    statusLabel.setText("Nodo non valido/non accessibile.");
                    LOGGER.warn("Nodo non valido/non accessibile: {}", input, ex);
                    JOptionPane.showMessageDialog(App.this, "Nodo non valido o non accessibile:\n" + ex.getMessage(), "Errore", JOptionPane.ERROR_MESSAGE);
                } finally {
                    validatePathButton.setEnabled(true);
                }
            }
        };

        worker.execute();
    }

    private void loadRootNodes() {
        logArea.append("Caricamento nodi radice...\n");
        SwingWorker<DefaultMutableTreeNode, String> worker = new SwingWorker<DefaultMutableTreeNode, String>() {
            @Override
            protected DefaultMutableTreeNode doInBackground() throws Exception {
                Folder rootFolder = cmisService.getSession().getRootFolder();
                publish("Trovata radice: " + rootFolder.getName() + " [" + rootFolder.getId() + "]");
                
                DefaultMutableTreeNode root = new DefaultMutableTreeNode(new NodeInfo(
                        rootFolder.getId(), 
                        rootFolder.getName(), 
                        rootFolder.getType().getId(), 
                        rootFolder.getPath(),
                        true));
                
                List<NodeInfo> childrenL1 = cmisService.getChildren(rootFolder);
                for (NodeInfo child1 : childrenL1) {
                    if (child1.isFolder()) {
                        publish("  [+] INSERITO (L1): " + child1.getName() + " (" + child1.getType() + ")");
                        DefaultMutableTreeNode nodeL1 = new DefaultMutableTreeNode(child1);
                        root.add(nodeL1);
                        
                        try {
                            List<NodeInfo> childrenL2 = cmisService.getChildren(child1.getId());
                            for (NodeInfo child2 : childrenL2) {
                                if (child2.isFolder()) {
                                    publish("    [+] INSERITO (L2): " + child2.getName() + " (" + child2.getType() + ")");
                                    nodeL1.add(new DefaultMutableTreeNode(child2));
                                } else {
                                    publish("    [-] ESCLUSO (L2): " + child2.getName() + " (" + child2.getType() + ") - non è una cartella");
                                }
                            }
                        } catch (Exception e) {
                            publish("    [!] ERRORE L2 per " + child1.getName() + ": " + e.getMessage());
                        }
                    } else {
                        publish("  [-] ESCLUSO (L1): " + child1.getName() + " (" + child1.getType() + ") - non è una cartella");
                    }
                }
                return root;
            }

            @Override
            protected void process(List<String> chunks) {
                for (String log : chunks) {
                    logArea.append(log + "\n");
                }
                logArea.setCaretPosition(logArea.getDocument().getLength());
            }

            @Override
            protected void done() {
                try {
                    DefaultMutableTreeNode newRoot = get();
                    rootNode.removeAllChildren();
                    rootNode.setUserObject(newRoot.getUserObject());
                    while (newRoot.getChildCount() > 0) {
                        rootNode.add((DefaultMutableTreeNode) newRoot.getChildAt(0));
                    }
                    treeModel.nodeStructureChanged(rootNode);
                    for (int i = 0; i < nodeTree.getRowCount(); i++) nodeTree.expandRow(i);
                    statusLabel.setText("Nodi caricati.");
                } catch (Exception ex) {
                    statusLabel.setText("Errore caricamento.");
                } finally {
                    connectButton.setEnabled(true);
                }
            }
        };
        worker.execute();
    }

    private void startExport() {
        NodeInfo nodeInfo = manualStartNode;
        if (nodeInfo == null) {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) nodeTree.getLastSelectedPathComponent();
            if (selectedNode == null) return;
            nodeInfo = (NodeInfo) selectedNode.getUserObject();
        }

        int depth = (Integer) depthSpinner.getValue();

        File csvFile;
        if (csvNameDirty) {
            csvFile = resolveManualCsvFile(csvNameField.getText().trim());
            if (csvFile == null) {
                return;
            }
            if (csvFile.exists()) {
                JOptionPane.showMessageDialog(this, "Il file esiste già. Scegli un nome diverso:\n" + csvFile.getAbsolutePath(), "Conflitto", JOptionPane.WARNING_MESSAGE);
                return;
            }
        } else {
            File outputDir = new File(".");
            csvFile = generateExportCsvFile(outputDir, nodeInfo.getName(), nodeInfo.getId(), depth);
            if (csvFile == null) {
                JOptionPane.showMessageDialog(this, "Impossibile generare il file CSV. Verificare permessi e spazio su disco.", "Errore", JOptionPane.ERROR_MESSAGE);
                return;
            }
            setCsvNameFieldValue(csvFile.getName());
        }

        exportCancelRequested.set(false);
        exportButton.setEnabled(false);
        cancelExportButton.setEnabled(true);
        logArea.append("Avvio esportazione per il nodo: " + nodeInfo.getName() + "\n");
        statusLabel.setText("Esportazione in corso...");
        LOGGER.info("Esportazione richiesta: nodeId={}, folderName={}, depth={}, output={}", nodeInfo.getId(), nodeInfo.getName(), depth, csvFile.getAbsolutePath());

        final NodeInfo exportNodeInfo = nodeInfo;
        final int exportDepth = depth;
        final File exportCsvFile = csvFile;

        exportWorker = new SwingWorker<Boolean, String>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return cmisService.exportPermissionsRecursive(
                        exportNodeInfo.getId(),
                        exportDepth,
                        exportCsvFile.getAbsolutePath(),
                        this::publish,
                        () -> exportCancelRequested.get()
                );
            }

            @Override
            protected void process(List<String> chunks) {
                for (String log : chunks) {
                    logArea.append(log + "\n");
                    logArea.setCaretPosition(logArea.getDocument().getLength());
                }
            }

            @Override
            protected void done() {
                try {
                    boolean cancelled = get();
                    File out = exportCsvFile.getAbsoluteFile();
                    if (cancelled) {
                        logArea.append("--- Esportazione interrotta. Risultati parziali salvati. ---\n");
                        statusLabel.setText("Esportazione interrotta (parziale).");
                        LOGGER.warn("Esportazione interrotta: output={}", out);
                        JOptionPane.showMessageDialog(
                                App.this,
                                "Esportazione interrotta.\nRisultati parziali salvati in:\n" + out,
                                "Interrotta",
                                JOptionPane.WARNING_MESSAGE
                        );
                    } else {
                        logArea.append("--- Esportazione completata con successo! ---\n");
                        statusLabel.setText("Esportazione completata.");
                        LOGGER.info("Esportazione completata: output={}", out);
                        JOptionPane.showMessageDialog(App.this, "Esportazione completata: " + out);
                    }
                } catch (Exception ex) {
                    logArea.append("ERRORE: " + ex.getMessage() + "\n");
                    JOptionPane.showMessageDialog(App.this, "Errore durante l'esportazione: " + ex.getMessage());
                    statusLabel.setText("Errore durante l'esportazione.");
                    LOGGER.error("Errore durante l'esportazione", ex);
                } finally {
                    exportWorker = null;
                    exportButton.setEnabled(true);
                    cancelExportButton.setEnabled(false);
                }
            }
        };
        exportWorker.execute();
    }

    private File resolveManualCsvFile(String input) {
        if (input == null || input.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Inserire un nome per il file CSV.", "Validazione", JOptionPane.WARNING_MESSAGE);
            return null;
        }

        File candidate = new File(input);
        if (input.endsWith("\\") || input.endsWith("/") || candidate.isDirectory()) {
            JOptionPane.showMessageDialog(this, "Inserire un nome file CSV, non solo una cartella.", "Validazione", JOptionPane.WARNING_MESSAGE);
            return null;
        }

        String fileName = candidate.getName();
        if (INVALID_WINDOWS_CHARS.matcher(fileName).find()) {
            JOptionPane.showMessageDialog(this, "Il nome file contiene caratteri non validi per Windows.", "Validazione", JOptionPane.WARNING_MESSAGE);
            return null;
        }

        if (!fileName.toLowerCase().endsWith(".csv")) {
            File parentFile = candidate.getParentFile();
            candidate = (parentFile == null) ? new File(fileName + ".csv") : new File(parentFile, fileName + ".csv");
        }

        File parent = candidate.getParentFile();
        if (parent != null && !parent.exists()) {
            try {
                if (!parent.mkdirs()) {
                    JOptionPane.showMessageDialog(this, "Impossibile creare la cartella di destinazione:\n" + parent.getAbsolutePath(), "Errore", JOptionPane.ERROR_MESSAGE);
                    return null;
                }
            } catch (SecurityException e) {
                JOptionPane.showMessageDialog(this, "Permessi insufficienti sulla cartella di destinazione:\n" + parent.getAbsolutePath(), "Errore", JOptionPane.ERROR_MESSAGE);
                LOGGER.error("Permessi insufficienti per creare cartella output: {}", parent.getAbsolutePath(), e);
                return null;
            }
        }

        return candidate;
    }

    private File generateExportCsvFile(File dir, String sourceFolderName, String nodeId, int depth) {
        try {
            if (dir == null) {
                dir = new File(".");
            }

            if (!dir.exists() && !dir.mkdirs()) {
                LOGGER.warn("Directory output non creata: {}", dir.getAbsolutePath());
                return null;
            }

            String baseName = ExportFileNameUtils.buildExportFileName(sourceFolderName, LocalDateTime.now(), nodeId, depth);
            if (baseName.toLowerCase().endsWith(".csv")) {
                baseName = baseName.substring(0, baseName.length() - 4);
            }

            File candidate = new File(dir, baseName + ".csv");
            int suffix = 1;
            while (candidate.exists()) {
                candidate = new File(dir, baseName + "_" + suffix + ".csv");
                suffix++;
            }
            return candidate;
        } catch (SecurityException e) {
            LOGGER.error("Permessi insufficienti per creare file in output", e);
            return null;
        }
    }

    private String sanitizeFileNamePart(String s) {
        return ExportFileNameUtils.sanitizeFileNamePart(s);
    }

    private void requestCancelExport() {
        if (exportWorker == null) {
            return;
        }
        exportCancelRequested.set(true);
        cancelExportButton.setEnabled(false);
        statusLabel.setText("Interruzione richiesta... attendo chiusura.");
        logArea.append("[STOP] Richiesta interruzione ricevuta. Interrompo l'elaborazione...\n");
    }

    public static void main(String[] args) {
        FlatLightLaf.setup();
        SwingUtilities.invokeLater(() -> new App().setVisible(true));
    }
}

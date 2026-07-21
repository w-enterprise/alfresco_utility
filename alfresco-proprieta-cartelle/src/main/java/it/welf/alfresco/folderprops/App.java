package it.welf.alfresco.folderprops;

import com.formdev.flatlaf.FlatLightLaf;
import it.welf.alfresco.folderprops.model.ConfigManager;
import it.welf.alfresco.folderprops.model.NodeInfo;
import it.welf.alfresco.folderprops.service.AlfrescoService;
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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class App extends JFrame {
    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

    private JTextField addressField;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton connectButton;
    private JTree nodeTree;
    private DefaultTreeModel treeModel;
    private DefaultMutableTreeNode rootNode;
    private AlfrescoService alfrescoService;
    private ConfigManager configManager;
    private JLabel statusLabel;
    
    private JSpinner depthSpinner;
    private JTextField reportNameField;
    private JButton generateButton;
    private JButton cancelButton;
    private JTextArea logArea;
    private JLabel selectedFolderNameLabel;
    private JLabel selectedNodeIdLabel;
    private JButton copyButton;
    private JTextField startNodePathField;
    private JButton validatePathButton;
    private NodeInfo manualStartNode;
    
    private JButton selectAllButton;
    private JButton deselectAllButton;
    private JLabel selectionCounterLabel;
    private JProgressBar overallProgressBar;

    private SwingWorker<Boolean, String> generateWorker;
    private final AtomicBoolean cancelRequested = new AtomicBoolean(false);

    private boolean reportNameDirty = false;
    private boolean reportNameProgrammaticUpdate = false;

    private static final String CMIS_PATH = "/alfresco/api/-default-/public/cmis/versions/1.1/atom";
    private static final int MAX_LOG_LINES = 1000;

    public App() {
        setTitle("Alfresco Proprieta Cartelle");
        setSize(1100, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        alfrescoService = new AlfrescoService();
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
        nodeTree.setCellRenderer(new CheckBoxTreeCellRenderer());
        nodeTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        
        JPanel treeControls = new JPanel(new BorderLayout(5, 5));
        JPanel selectionButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        selectAllButton = new JButton("Seleziona Tutti");
        deselectAllButton = new JButton("Deseleziona Tutti");
        selectionCounterLabel = new JLabel("Selezionati: 0");
        selectionButtons.add(selectAllButton);
        selectionButtons.add(deselectAllButton);
        selectionButtons.add(selectionCounterLabel);
        treeControls.add(selectionButtons, BorderLayout.WEST);

        JPanel treeContainer = new JPanel(new BorderLayout());
        treeContainer.add(treeControls, BorderLayout.NORTH);
        JScrollPane treeScroll = new JScrollPane(nodeTree);
        treeScroll.setBorder(BorderFactory.createTitledBorder("Esplora e Seleziona Nodi"));
        treeContainer.add(treeScroll, BorderLayout.CENTER);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Log Operazioni"));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, treeContainer, logScroll);
        splitPane.setDividerLocation(400);
        mainPanel.add(splitPane, BorderLayout.CENTER);

        // --- Pannello Destro: Configurazione Report ---
        JPanel rightPanel = new JPanel(new GridBagLayout());
        rightPanel.setPreferredSize(new Dimension(320, 0));
        rightPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Opzioni Report"),
                new EmptyBorder(10, 10, 10, 10)));

        GridBagConstraints rGbc = new GridBagConstraints();
        rGbc.insets = new Insets(5, 5, 5, 5);
        rGbc.fill = GridBagConstraints.HORIZONTAL;
        rGbc.gridx = 0; rGbc.gridy = 0;

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
        rightPanel.add(new JLabel("Nome File Report:"), rGbc);
        rGbc.gridy = 11;
        String defaultFileName = "report_analisi_alfresco_" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".txt";
        reportNameField = new JTextField(defaultFileName);
        rightPanel.add(reportNameField, rGbc);
        reportNameField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { onReportNameChanged(); }
            @Override
            public void removeUpdate(DocumentEvent e) { onReportNameChanged(); }
            @Override
            public void changedUpdate(DocumentEvent e) { onReportNameChanged(); }
        });

        rGbc.gridy = 12;
        rGbc.weighty = 0.0;
        rGbc.anchor = GridBagConstraints.NORTH;
        rGbc.insets = new Insets(20, 5, 5, 5);
        generateButton = new JButton("Genera Report");
        generateButton.setEnabled(false);
        generateButton.setFont(generateButton.getFont().deriveFont(Font.BOLD));
        rightPanel.add(generateButton, rGbc);

        rGbc.gridy = 13;
        rGbc.insets = new Insets(5, 5, 5, 5);
        cancelButton = new JButton("Annulla Operazione");
        cancelButton.setEnabled(false);
        cancelButton.setBackground(new Color(220, 53, 69));
        cancelButton.setForeground(Color.WHITE);
        rightPanel.add(cancelButton, rGbc);

        rGbc.gridy = 14;
        overallProgressBar = new JProgressBar(0, 100);
        overallProgressBar.setStringPainted(true);
        overallProgressBar.setVisible(false);
        rightPanel.add(overallProgressBar, rGbc);

        rGbc.gridy = 15;
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
                    generateButton.setEnabled(alfrescoService.getSession() != null);
                    if (ni.getPath() != null && !ni.getPath().trim().isEmpty()) {
                        startNodePathField.setText(ni.getPath());
                    }
                    updateReportNameFieldAuto(ni.getName());
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

        generateButton.addActionListener(e -> startReportGeneration());
        cancelButton.addActionListener(e -> requestCancel());
        validatePathButton.addActionListener(e -> validateAndUseStartPath());

        selectAllButton.addActionListener(e -> toggleAllSelection(true));
        deselectAllButton.addActionListener(e -> toggleAllSelection(false));

        nodeTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int row = nodeTree.getRowForLocation(e.getX(), e.getY());
                if (row != -1) {
                    TreePath path = nodeTree.getPathForRow(row);
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                    Object obj = node.getUserObject();
                    if (obj instanceof NodeInfo) {
                        NodeInfo ni = (NodeInfo) obj;
                        if (e.getX() < nodeTree.getPathBounds(path).x + 20) {
                            ni.setSelected(!ni.isSelected());
                            treeModel.nodeChanged(node);
                            updateSelectionCounter();
                        }
                    }
                }
            }
        });

        loadSavedConfig();
    }

    private void updateSelectionCounter() {
        int count = getSelectedNodes().size();
        selectionCounterLabel.setText("Selezionati: " + count);
        if (generateWorker == null) {
            generateButton.setEnabled(count > 0 && alfrescoService.getSession() != null);
        }
    }

    private void toggleAllSelection(boolean selected) {
        toggleNodeSelectionRecursive(rootNode, selected);
        treeModel.nodeStructureChanged(rootNode);
        updateSelectionCounter();
    }

    private void toggleNodeSelectionRecursive(DefaultMutableTreeNode node, boolean selected) {
        Object obj = node.getUserObject();
        if (obj instanceof NodeInfo) {
            ((NodeInfo) obj).setSelected(selected);
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            toggleNodeSelectionRecursive((DefaultMutableTreeNode) node.getChildAt(i), selected);
        }
    }

    private List<NodeInfo> getSelectedNodes() {
        List<NodeInfo> selectedNodes = new java.util.ArrayList<>();
        collectSelectedNodes(rootNode, selectedNodes);
        return selectedNodes;
    }

    private void collectSelectedNodes(DefaultMutableTreeNode node, List<NodeInfo> selectedNodes) {
        Object obj = node.getUserObject();
        if (obj instanceof NodeInfo) {
            NodeInfo ni = (NodeInfo) obj;
            if (ni.isSelected()) {
                selectedNodes.add(ni);
            }
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            collectSelectedNodes((DefaultMutableTreeNode) node.getChildAt(i), selectedNodes);
        }
    }

    private void appendLog(String message) {
        logArea.append(message + "\n");
        
        if (logArea.getLineCount() > MAX_LOG_LINES) {
            try {
                int end = logArea.getLineEndOffset(logArea.getLineCount() - MAX_LOG_LINES);
                logArea.replaceRange("", 0, end);
            } catch (Exception e) {
            }
        }
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private void onReportNameChanged() {
        if (reportNameProgrammaticUpdate) return;
        if (!reportNameDirty) {
            reportNameDirty = true;
            LOGGER.info("Campo nome report modificato manualmente");
        }
    }

    private void updateReportNameFieldAuto(String folderName) {
        if (reportNameDirty) return;
        String nodeId = (manualStartNode != null) ? manualStartNode.getId() : selectedNodeIdLabel.getText();
        int depth = (Integer) depthSpinner.getValue();
        String autoName = FileNameUtils.buildReportFileName(folderName, LocalDateTime.now(), nodeId, depth);
        setReportNameFieldValue(autoName);
    }

    private void setReportNameFieldValue(String value) {
        reportNameProgrammaticUpdate = true;
        try {
            reportNameField.setText(value);
        } finally {
            reportNameProgrammaticUpdate = false;
        }
    }

    private void resetSelectionLabels() {
        selectedFolderNameLabel.setText("-");
        selectedNodeIdLabel.setText("-");
        copyButton.setEnabled(false);
        generateButton.setEnabled(false);
        cancelButton.setEnabled(false);
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
                alfrescoService.connect(fullUrl, username, password);
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
        if (alfrescoService.getSession() == null) {
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
        generateButton.setEnabled(false);

        SwingWorker<NodeInfo, Void> worker = new SwingWorker<NodeInfo, Void>() {
            @Override
            protected NodeInfo doInBackground() {
                String trimmed = input.trim();
                Folder folder = trimmed.startsWith("/") ? alfrescoService.getFolderByPath(trimmed) : alfrescoService.getFolderById(trimmed);
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
                    if (generateWorker == null) {
                        generateButton.setEnabled(true);
                    }
                    statusLabel.setText("Nodo valido selezionato.");
                    if (ni.getPath() != null && !ni.getPath().trim().isEmpty()) {
                        startNodePathField.setText(ni.getPath());
                    }
                    updateReportNameFieldAuto(ni.getName());
                    LOGGER.info("Nodo valido: id={}, name={}, path={}", ni.getId(), ni.getName(), ni.getPath());
                } catch (Exception ex) {
                    manualStartNode = null;
                    if (generateWorker == null) {
                        generateButton.setEnabled(false);
                    }
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
        appendLog("Caricamento nodi radice...");
        SwingWorker<DefaultMutableTreeNode, String> worker = new SwingWorker<DefaultMutableTreeNode, String>() {
            @Override
            protected DefaultMutableTreeNode doInBackground() throws Exception {
                Folder rootFolder = alfrescoService.getSession().getRootFolder();
                publish("Trovata radice: " + rootFolder.getName() + " [" + rootFolder.getId() + "]");
                
                DefaultMutableTreeNode root = new DefaultMutableTreeNode(new NodeInfo(
                        rootFolder.getId(), 
                        rootFolder.getName(), 
                        rootFolder.getType().getId(), 
                        rootFolder.getPath(),
                        true));
                
                List<NodeInfo> childrenL1 = alfrescoService.getChildren(rootFolder);
                for (NodeInfo child1 : childrenL1) {
                    if (child1.isFolder()) {
                        publish("  [+] INSERITO (L1): " + child1.getName() + " (" + child1.getType() + ")");
                        DefaultMutableTreeNode nodeL1 = new DefaultMutableTreeNode(child1);
                        root.add(nodeL1);
                        
                        try {
                            List<NodeInfo> childrenL2 = alfrescoService.getChildren(child1.getId());
                            for (NodeInfo child2 : childrenL2) {
                                if (child2.isFolder()) {
                                    publish("    [+] INSERITO (L2): " + child2.getName() + " (" + child2.getType() + ")");
                                    nodeL1.add(new DefaultMutableTreeNode(child2));
                                }
                            }
                        } catch (Exception e) {
                            publish("    [!] ERRORE L2 per " + child1.getName() + ": " + e.getMessage());
                        }
                    }
                }
                return root;
            }

            @Override
            protected void process(List<String> chunks) {
                for (String log : chunks) {
                    appendLog(log);
                }
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

    private void startReportGeneration() {
        List<NodeInfo> selectedNodes = getSelectedNodes();
        if (selectedNodes.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Selezionare almeno una cartella tramite le checkbox.", "Nessuna selezione", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int depth = (Integer) depthSpinner.getValue();
        File outputDir = new File(".");

        long freeSpace = outputDir.getFreeSpace();
        if (freeSpace < 1024 * 1024 * 50) {
            int choice = JOptionPane.showConfirmDialog(this, 
                "Lo spazio su disco sembra scarso (" + (freeSpace / 1024 / 1024) + " MB). Continuare comunque?", 
                "Spazio Disco", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (choice != JOptionPane.YES_OPTION) return;
        }

        cancelRequested.set(false);
        generateButton.setEnabled(false);
        cancelButton.setEnabled(true);
        selectAllButton.setEnabled(false);
        deselectAllButton.setEnabled(false);
        overallProgressBar.setValue(0);
        overallProgressBar.setVisible(true);
        
        appendLog("Avvio generazione report per " + selectedNodes.size() + " cartelle...");
        statusLabel.setText("Generazione report in corso...");

        generateWorker = new SwingWorker<Boolean, String>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                int total = selectedNodes.size();
                int current = 0;
                List<String> results = new java.util.ArrayList<>();

                for (NodeInfo nodeInfo : selectedNodes) {
                    if (cancelRequested.get()) break;

                    current++;
                    String progressMsg = String.format("[%d/%d] Elaborando: %s", current, total, nodeInfo.getName());
                    publish(progressMsg);
                    LOGGER.info("Inizio elaborazione: {} ({})", nodeInfo.getName(), nodeInfo.getId());

                    File reportFile = generateReportFile(outputDir, nodeInfo.getName(), nodeInfo.getId(), depth);
                    if (reportFile == null) {
                        publish("ERRORE: Impossibile creare il file per " + nodeInfo.getName());
                        final int progress = (int) (((double) current / total) * 100);
                        SwingUtilities.invokeLater(() -> overallProgressBar.setValue(progress));
                        continue;
                    }

                    try {
                        boolean cancelled = alfrescoService.exportReportRecursive(
                                nodeInfo.getId(),
                                depth,
                                reportFile.getAbsolutePath(),
                                msg -> publish("  > " + msg),
                                () -> cancelRequested.get()
                        );

                        if (cancelled) {
                            publish("Interrotto: " + nodeInfo.getName());
                            results.add("INTERROTTO: " + nodeInfo.getName() + " -> " + reportFile.getName());
                            break;
                        } else {
                            results.add("OK: " + nodeInfo.getName() + " -> " + reportFile.getName());
                        }
                    } catch (Exception e) {
                        String errMsg = "ERRORE su " + nodeInfo.getName() + ": " + e.getMessage();
                        publish(errMsg);
                        LOGGER.error(errMsg, e);
                        results.add("FALLITO: " + nodeInfo.getName() + " (" + e.getMessage() + ")");
                    } finally {
                        final int progress = (int) (((double) current / total) * 100);
                        SwingUtilities.invokeLater(() -> overallProgressBar.setValue(progress));
                    }
                }

                publish("\n--- RIEPILOGO FINALE ---");
                for (String res : results) {
                    publish(res);
                }
                publish("------------------------\n");

                return cancelRequested.get();
            }

            @Override
            protected void process(List<String> chunks) {
                for (String log : chunks) {
                    appendLog(log);
                }
            }

            @Override
            protected void done() {
                try {
                    boolean cancelled = get();
                    if (cancelled) {
                        statusLabel.setText("Generazione interrotta.");
                        JOptionPane.showMessageDialog(App.this, "Generazione interrotta dall'utente.", "Interrotta", JOptionPane.WARNING_MESSAGE);
                    } else {
                        statusLabel.setText("Generazione completata.");
                        JOptionPane.showMessageDialog(App.this, "Tutti i report sono stati generati. Controlla il log per i dettagli.");
                    }
                } catch (Exception ex) {
                    appendLog("ERRORE CRITICO: " + ex.getMessage());
                    statusLabel.setText("Errore durante la generazione.");
                } finally {
                    generateWorker = null;
                    generateButton.setEnabled(true);
                    cancelButton.setEnabled(false);
                    selectAllButton.setEnabled(true);
                    deselectAllButton.setEnabled(true);
                    overallProgressBar.setVisible(false);
                }
            }
        };
        generateWorker.execute();
    }

    private File generateReportFile(File dir, String sourceFolderName, String nodeId, int depth) {
        try {
            if (dir == null) {
                dir = new File(".");
            }

            if (!dir.exists() && !dir.mkdirs()) {
                LOGGER.warn("Directory output non creata: {}", dir.getAbsolutePath());
                return null;
            }

            String baseName = FileNameUtils.buildReportFileName(sourceFolderName, LocalDateTime.now(), nodeId, depth);
            File candidate = new File(dir, baseName);
            int suffix = 1;
            while (candidate.exists()) {
                String nameWithoutExt = baseName.substring(0, baseName.lastIndexOf('.'));
                String ext = baseName.substring(baseName.lastIndexOf('.'));
                candidate = new File(dir, nameWithoutExt + "_" + suffix + ext);
                suffix++;
            }
            return candidate;
        } catch (Exception e) {
            LOGGER.error("Errore creazione file report", e);
            return null;
        }
    }

    private void requestCancel() {
        if (generateWorker == null) {
            return;
        }
        cancelRequested.set(true);
        cancelButton.setEnabled(false);
        statusLabel.setText("Interruzione richiesta... attendo chiusura.");
        appendLog("[STOP] Richiesta interruzione ricevuta. Interrompo l'elaborazione...");
    }

    public static void main(String[] args) {
        FlatLightLaf.setup();
        SwingUtilities.invokeLater(() -> new App().setVisible(true));
    }

    private static class CheckBoxTreeCellRenderer extends JPanel implements javax.swing.tree.TreeCellRenderer {
        private final JCheckBox checkBox;
        private final JLabel label;

        public CheckBoxTreeCellRenderer() {
            super(new BorderLayout());
            setOpaque(false);
            checkBox = new JCheckBox();
            label = new JLabel();
            add(checkBox, BorderLayout.WEST);
            add(label, BorderLayout.CENTER);
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
            Object obj = node.getUserObject();
            
            if (obj instanceof NodeInfo) {
                NodeInfo ni = (NodeInfo) obj;
                checkBox.setVisible(true);
                checkBox.setSelected(ni.isSelected());
                label.setText(ni.getName());
                label.setIcon(javax.swing.UIManager.getIcon(ni.isFolder() ? "Tree.closedIcon" : "Tree.leafIcon"));
            } else {
                checkBox.setVisible(false);
                label.setText(value.toString());
            }

            if (selected) {
                label.setForeground(javax.swing.UIManager.getColor("Tree.selectionForeground"));
                setBackground(javax.swing.UIManager.getColor("Tree.selectionBackground"));
            } else {
                label.setForeground(javax.swing.UIManager.getColor("Tree.textForeground"));
                setBackground(javax.swing.UIManager.getColor("Tree.textBackground"));
            }

            return this;
        }
    }
}

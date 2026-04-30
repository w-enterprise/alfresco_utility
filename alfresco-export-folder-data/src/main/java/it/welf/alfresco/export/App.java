package it.welf.alfresco.export;

import com.formdev.flatlaf.FlatLightLaf;
import it.welf.alfresco.export.model.ConfigManager;
import it.welf.alfresco.export.model.NodeInfo;
import it.welf.alfresco.export.service.CmisService;
import org.apache.chemistry.opencmis.client.api.Folder;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class App extends JFrame {
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
    private JTextArea logArea;
    private JLabel selectedFolderNameLabel;
    private JLabel selectedNodeIdLabel;
    private JButton copyButton;

    private static final String CMIS_PATH = "/alfresco/api/-default-/public/cmis/versions/1.1/atom";

    public App() {
        setTitle("Alfresco Node Explorer & Permission Exporter");
        setSize(1000, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        cmisService = new CmisService();
        configManager = new ConfigManager();

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
        rightPanel.add(new JLabel("Profondità (0=tutti):"), rGbc);
        rGbc.gridy = 6;
        depthSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
        rightPanel.add(depthSpinner, rGbc);

        rGbc.gridy = 7;
        rightPanel.add(new JLabel("Nome File CSV:"), rGbc);
        rGbc.gridy = 8;
        String defaultFileName = "export_permissions_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";
        csvNameField = new JTextField(defaultFileName);
        rightPanel.add(csvNameField, rGbc);

        rGbc.gridy = 9;
        rGbc.weighty = 1.0;
        rGbc.anchor = GridBagConstraints.NORTH;
        rGbc.insets = new Insets(20, 5, 5, 5);
        exportButton = new JButton("Avvia Esportazione");
        exportButton.setEnabled(false);
        exportButton.setFont(exportButton.getFont().deriveFont(Font.BOLD));
        rightPanel.add(exportButton, rGbc);

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
                    selectedFolderNameLabel.setText(ni.getName());
                    selectedNodeIdLabel.setText(ni.getId());
                    copyButton.setEnabled(true);
                    exportButton.setEnabled(cmisService.getSession() != null);
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

        loadSavedConfig();
    }

    private void resetSelectionLabels() {
        selectedFolderNameLabel.setText("-");
        selectedNodeIdLabel.setText("-");
        copyButton.setEnabled(false);
        exportButton.setEnabled(false);
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
                    loadRootNodes();
                } catch (Exception ex) {
                    statusLabel.setText("Errore connessione.");
                    JOptionPane.showMessageDialog(App.this, "Errore: " + ex.getMessage(), "Errore", JOptionPane.ERROR_MESSAGE);
                    connectButton.setEnabled(true);
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
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) nodeTree.getLastSelectedPathComponent();
        if (selectedNode == null) return;
        
        NodeInfo nodeInfo = (NodeInfo) selectedNode.getUserObject();
        int depth = (Integer) depthSpinner.getValue();
        String csvFile = csvNameField.getText().trim();
        
        if (csvFile.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Inserire un nome per il file CSV.");
            return;
        }

        exportButton.setEnabled(false);
        logArea.append("Avvio esportazione per il nodo: " + nodeInfo.getName() + "\n");

        SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                cmisService.exportPermissionsRecursive(nodeInfo.getId(), depth, csvFile, this::publish);
                return null;
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
                    get();
                    logArea.append("--- Esportazione completata con successo! ---\n");
                    JOptionPane.showMessageDialog(App.this, "Esportazione completata: " + new File(csvFile).getAbsolutePath());
                } catch (Exception ex) {
                    logArea.append("ERRORE: " + ex.getMessage() + "\n");
                    JOptionPane.showMessageDialog(App.this, "Errore durante l'esportazione: " + ex.getMessage());
                } finally {
                    exportButton.setEnabled(true);
                }
            }
        };
        worker.execute();
    }

    public static void main(String[] args) {
        FlatLightLaf.setup();
        SwingUtilities.invokeLater(() -> new App().setVisible(true));
    }
}
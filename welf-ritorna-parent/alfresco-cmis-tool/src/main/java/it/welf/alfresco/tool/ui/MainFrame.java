package it.welf.alfresco.tool.ui;

import it.welf.alfresco.tool.model.NodeInfo;
import it.welf.alfresco.tool.service.CmisService;
import it.welf.alfresco.tool.util.ConfigManager;
import org.apache.chemistry.opencmis.client.api.Session;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class MainFrame extends JFrame {
    private JTextField txtUrl;
    private JTextField txtUser;
    private JPasswordField txtPass;
    private JTextField txtNodeId;
    private JTextArea txtLog;
    private JButton btnSearch;
    
    private final ConfigManager configManager;
    private final CmisService cmisService;

    public MainFrame() {
        configManager = new ConfigManager();
        cmisService = new CmisService();
        
        initUI();
        loadConfig();
    }

    private void initUI() {
        setTitle("Alfresco-Ritorna-ParentFolder");
        setSize(600, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Main layout
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Input Panel
        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // URL
        gbc.gridx = 0; gbc.gridy = 0;
        inputPanel.add(new JLabel("CMIS URL:"), gbc);
        
        txtUrl = new JTextField(30);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        inputPanel.add(txtUrl, gbc);

        // User
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        inputPanel.add(new JLabel("Username:"), gbc);
        
        txtUser = new JTextField(20);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0;
        inputPanel.add(txtUser, gbc);

        // Pass
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        inputPanel.add(new JLabel("Password:"), gbc);
        
        txtPass = new JPasswordField(20);
        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 1.0;
        inputPanel.add(txtPass, gbc);

        // Separator
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        inputPanel.add(new JSeparator(), gbc);

        // Node ID
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 1;
        inputPanel.add(new JLabel("Target Node ID:"), gbc);
        
        txtNodeId = new JTextField(30);
        gbc.gridx = 1; gbc.gridy = 4;
        inputPanel.add(txtNodeId, gbc);

        // Button
        btnSearch = new JButton("Find Parent Node");
        gbc.gridx = 1; gbc.gridy = 5; gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        inputPanel.add(btnSearch, gbc);

        mainPanel.add(inputPanel, BorderLayout.NORTH);

        // Log/Result Area
        txtLog = new JTextArea();
        txtLog.setEditable(false);
        txtLog.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(txtLog);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Output / Log"));
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        add(mainPanel);

        // Action
        btnSearch.addActionListener(this::onSearch);
    }

    private void loadConfig() {
        configManager.load();
        if (configManager.getUrl() != null) txtUrl.setText(configManager.getUrl());
        if (configManager.getUsername() != null) txtUser.setText(configManager.getUsername());
        if (configManager.getPassword() != null) txtPass.setText(configManager.getPassword());
    }

    private void onSearch(ActionEvent e) {
        String url = txtUrl.getText().trim();
        String user = txtUser.getText().trim();
        String pass = new String(txtPass.getPassword());
        String nodeId = txtNodeId.getText().trim();

        if (url.isEmpty() || user.isEmpty() || nodeId.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill all fields (URL, User, Node ID)", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        btnSearch.setEnabled(false);
        txtLog.setText("Connecting to Alfresco...\n");

        // Run in background
        SwingWorker<NodeInfo, String> worker = new SwingWorker<NodeInfo, String>() {
            @Override
            protected NodeInfo doInBackground() throws Exception {
                publish("Establishing session...");
                Session session = cmisService.createSession(url, user, pass);
                
                publish("Session established. Fetching node: " + nodeId);
                NodeInfo parent = cmisService.getParentNode(session, nodeId);
                
                return parent;
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                for (String msg : chunks) {
                    txtLog.append(msg + "\n");
                }
            }

            @Override
            protected void done() {
                try {
                    NodeInfo result = get();
                    txtLog.append("\nSUCCESS!\n");
                    txtLog.append("--------------------------------------------------\n");
                    txtLog.append("Parent Node Found:\n");
                    txtLog.append(result.toString() + "\n");
                    txtLog.append("--------------------------------------------------\n");
                    
                    // Save config on success
                    configManager.save(url, user, pass);
                    txtLog.append("Configuration saved.");
                    
                } catch (Exception ex) {
                    txtLog.append("\nERROR: " + ex.getMessage() + "\n");
                    if (ex.getCause() != null) {
                         txtLog.append("Cause: " + ex.getCause().getMessage() + "\n");
                    }
                    ex.printStackTrace();
                } finally {
                    btnSearch.setEnabled(true);
                }
            }
        };

        worker.execute();
    }
}

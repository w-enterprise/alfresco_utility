package com.welf.estrainodi.view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * La vista dell'applicazione (GUI).
 * Implementata utilizzando Java Swing.
 */
public class AppView extends JFrame {

    private JTextField urlField;
    private JTextField userField;
    private JPasswordField passField;
    private JTextField nodeIdField;
    private JTextField maxItemsField;
    private JButton extractButton;
    private JTextArea logArea;
    private JProgressBar progressBar;

    /**
     * Costruttore della vista. Inizializza i componenti grafici.
     */
    public AppView() {
        super("Alfresco-Estrai-Nodi");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 500);
        setLocationRelativeTo(null); // Centra la finestra
        initComponents();
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // --- Configurazione Connessione ---
        JLabel urlLabel = new JLabel("CMIS URL:");
        urlField = new JTextField("http://localhost:8080/alfresco/api/-default-/public/cmis/versions/1.1/atom");
        
        JLabel userLabel = new JLabel("Username:");
        userField = new JTextField("admin");
        
        JLabel passLabel = new JLabel("Password:");
        passField = new JPasswordField("admin");

        // --- Input Node ID ---
        JLabel nodeIdLabel = new JLabel("Folder Node ID:");
        nodeIdField = new JTextField(30);

        JLabel maxItemsLabel = new JLabel("Max items (0 = tutti):");
        maxItemsField = new JTextField("0", 5);

        // --- Pulsante Azione ---
        extractButton = new JButton("Estrai Contenuti");

        // --- Output ---
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setVisible(false); // Nascosta inizialmente

        logArea = new JTextArea(10, 40);
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Log e Stato"));

        // Aggiunta componenti al layout
        // Riga 0: URL
        gbc.gridx = 0; gbc.gridy = 0;
        mainPanel.add(urlLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        mainPanel.add(urlField, gbc);

        // Riga 1: User
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        mainPanel.add(userLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0;
        mainPanel.add(userField, gbc);

        // Riga 2: Pass
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        mainPanel.add(passLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 1.0;
        mainPanel.add(passField, gbc);

        // Separatore
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        mainPanel.add(new JSeparator(), gbc);

        // Riga 4: Node ID
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 1; gbc.weightx = 0;
        mainPanel.add(nodeIdLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 4; gbc.weightx = 1.0;
        mainPanel.add(nodeIdField, gbc);

        // Riga 5: Max Items
        gbc.gridx = 0; gbc.gridy = 5; gbc.weightx = 0;
        mainPanel.add(maxItemsLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 5; gbc.weightx = 1.0;
        mainPanel.add(maxItemsField, gbc);

        // Riga 6: Button
        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2;
        mainPanel.add(extractButton, gbc);

        // Riga 7: Progress Bar
        gbc.gridx = 0; gbc.gridy = 7; gbc.gridwidth = 2;
        mainPanel.add(progressBar, gbc);

        // Riga 8: Log Area
        gbc.gridx = 0; gbc.gridy = 8; gbc.gridwidth = 2; gbc.weighty = 1.0; gbc.fill = GridBagConstraints.BOTH;
        mainPanel.add(scrollPane, gbc);

        add(mainPanel);
    }

    // --- Getters per il Controller ---

    public String getCmisUrl() { return urlField.getText(); }
    public String getUsername() { return userField.getText(); }
    public String getPassword() { return new String(passField.getPassword()); }
    public String getNodeId() { return nodeIdField.getText(); }
    public int getMaxItems() {
        try {
            return Integer.parseInt(maxItemsField.getText().trim());
        } catch (NumberFormatException e) {
            return 0; // Default a 0 se non valido
        }
    }

    // --- Setters per inizializzazione ---
    public void setCmisUrl(String url) { urlField.setText(url); }
    public void setUsername(String user) { userField.setText(user); }
    public void setPassword(String pass) { passField.setText(pass); }
    public void setNodeId(String nodeId) { nodeIdField.setText(nodeId); }
    public void setMaxItems(int maxItems) { maxItemsField.setText(String.valueOf(maxItems)); }

    public void setExtractButtonListener(ActionListener listener) {
        extractButton.addActionListener(listener);
    }

    public void appendLog(String message) {
        logArea.append(message + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    public void setProgressIndeterminate(boolean indeterminate) {
        progressBar.setVisible(true);
        progressBar.setIndeterminate(indeterminate);
    }

    public void stopProgress() {
        progressBar.setIndeterminate(false);
        progressBar.setValue(0);
        progressBar.setVisible(false);
    }

    public void showError(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
    }

    public void showSuccess(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE);
    }
    
    public void setControlsEnabled(boolean enabled) {
        extractButton.setEnabled(enabled);
        urlField.setEnabled(enabled);
        userField.setEnabled(enabled);
        passField.setEnabled(enabled);
        nodeIdField.setEnabled(enabled);
        maxItemsField.setEnabled(enabled);
    }
}

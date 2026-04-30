package com.alfresco.utility;

import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.text.DecimalFormat;
import java.util.concurrent.ExecutionException;

public class App extends JFrame {

    private JTextField urlField;
    private JTextField userField;
    private JPasswordField passField;
    private JCheckBox showPassCheck;
    private JButton connectButton;
    private JButton emptyButton;
    private JButton clearConfigButton;
    private JLabel statusLabel;
    private JLabel sizeLabel;
    private JProgressBar progressBar;
    private JTextArea logArea;
    
    private final ConfigManager configManager;
    private final CmisService cmisService;
    private boolean isCancelled = false;

    public App() {
        configManager = new ConfigManager();
        cmisService = new CmisService();
        
        initUI();
        loadConfig();
    }

    private void initUI() {
        setTitle("Alfresco Utility - Svuota Cestino");
        setSize(500, 450);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // Main Panel
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Input Fields
        JPanel inputPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        
        inputPanel.add(new JLabel("Server URL (e.g. .../alfresco):"));
        urlField = new JTextField("http://localhost:8080/alfresco");
        inputPanel.add(urlField);

        inputPanel.add(new JLabel("Username:"));
        userField = new JTextField("admin");
        inputPanel.add(userField);

        inputPanel.add(new JLabel("Password:"));
        passField = new JPasswordField();
        inputPanel.add(passField);
        
        inputPanel.add(new JLabel("")); // Spacer
        showPassCheck = new JCheckBox("Mostra Password");
        showPassCheck.addActionListener(e -> {
            if (showPassCheck.isSelected()) {
                passField.setEchoChar((char) 0);
            } else {
                passField.setEchoChar('•');
            }
        });
        inputPanel.add(showPassCheck);

        mainPanel.add(inputPanel);
        mainPanel.add(Box.createVerticalStrut(20));

        // Connect Button
        connectButton = new JButton("Connetti");
        connectButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        connectButton.addActionListener(this::onConnect);
        mainPanel.add(connectButton);
        
        mainPanel.add(Box.createVerticalStrut(10));
        
        // Config Button
        clearConfigButton = new JButton("Cancella Credenziali Salvate");
        clearConfigButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        clearConfigButton.addActionListener(e -> {
            configManager.clearConfig();
            JOptionPane.showMessageDialog(this, "Credenziali cancellate.");
            urlField.setText("http://localhost:8080/alfresco");
            userField.setText("");
        });
        mainPanel.add(clearConfigButton);

        mainPanel.add(Box.createVerticalStrut(20));

        // Status Area
        statusLabel = new JLabel("Stato: Disconnesso");
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statusLabel.setForeground(Color.RED);
        mainPanel.add(statusLabel);
        
        sizeLabel = new JLabel("Dimensione Cestino: -");
        sizeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        sizeLabel.setFont(new Font(sizeLabel.getFont().getName(), Font.BOLD, 14));
        mainPanel.add(sizeLabel);

        mainPanel.add(Box.createVerticalStrut(20));

        // Action Area
        emptyButton = new JButton("Svuota Cestino");
        emptyButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        emptyButton.setEnabled(false);
        emptyButton.setBackground(new Color(220, 53, 69));
        emptyButton.setForeground(Color.WHITE);
        emptyButton.addActionListener(this::onEmptyTrashcan);
        mainPanel.add(emptyButton);

        mainPanel.add(Box.createVerticalStrut(10));
        
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);
        mainPanel.add(progressBar);
        
        mainPanel.add(Box.createVerticalStrut(10));
        
        // Log Area
        logArea = new JTextArea(5, 40);
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Log Attività"));
        mainPanel.add(logScroll);

        add(mainPanel, BorderLayout.CENTER);
    }
    
    private void log(String message) {
        String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        logArea.append("[" + timestamp + "] " + message + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private void loadConfig() {
        ConfigManager.Config config = configManager.loadConfig();
        if (config != null) {
            if (config.serverUrl != null) urlField.setText(config.serverUrl);
            if (config.encryptedUsername != null) userField.setText(configManager.decryptUsername(config.encryptedUsername));
        }
    }

    private void onConnect(ActionEvent e) {
        String url = urlField.getText().trim();
        String user = userField.getText().trim();
        String pass = new String(passField.getPassword());

        if (url.isEmpty() || user.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Compilare tutti i campi.", "Errore", JOptionPane.ERROR_MESSAGE);
            return;
        }

        setLoading(true, "Connessione in corso...");
        log("Tentativo di connessione a: " + url);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                cmisService.connect(url, user, pass);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get(); // Check for exceptions
                    statusLabel.setText("Stato: Connesso");
                    statusLabel.setForeground(new Color(40, 167, 69));
                    emptyButton.setEnabled(true);
                    log("Connessione stabilita con successo.");
                    
                    // Save successful config
                    configManager.saveConfig(url, user);
                    
                    // Auto calculate size
                    calculateSize();
                    
                } catch (Exception ex) {
                    ex.printStackTrace();
                    statusLabel.setText("Stato: Errore Connessione");
                    statusLabel.setForeground(Color.RED);
                    log("Errore connessione: " + ex.getCause().getMessage());
                    JOptionPane.showMessageDialog(App.this, "Errore connessione: " + ex.getCause().getMessage(), "Errore", JOptionPane.ERROR_MESSAGE);
                } finally {
                    setLoading(false, null);
                }
            }
        };
        worker.execute();
    }

    private void calculateSize() {
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        progressBar.setString("Calcolo dimensione cestino...");
        sizeLabel.setText("Dimensione Cestino: Calcolo...");
        log("Avvio calcolo dimensione cestino...");
        isCancelled = false;

        SwingWorker<Long, String> worker = new SwingWorker<>() {
            @Override
            protected Long doInBackground() throws Exception {
                return cmisService.calculateTrashcanSize(new CmisService.ProgressCallback() {
                    @Override
                    public void onProgress(String message) {
                        publish(message);
                    }
                    @Override
                    public boolean isCancelled() {
                        return isCancelled;
                    }
                });
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                if (!chunks.isEmpty()) {
                    String msg = chunks.get(chunks.size() - 1);
                    progressBar.setString(msg);
                    log(msg);
                }
            }

            @Override
            protected void done() {
                try {
                    long size = get();
                    String sizeStr = formatSize(size);
                    sizeLabel.setText("Dimensione Cestino: " + sizeStr);
                    log("Calcolo completato. Dimensione: " + sizeStr);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    sizeLabel.setText("Dimensione Cestino: Errore");
                    log("Errore durante il calcolo dimensione: " + ex.getCause().getMessage());
                } finally {
                    progressBar.setIndeterminate(false);
                    progressBar.setVisible(false);
                }
            }
        };
        worker.execute();
    }

    private void onEmptyTrashcan(ActionEvent e) {
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Sei sicuro di voler svuotare il cestino?\nQuesta operazione non può essere annullata.", 
            "Conferma Svuotamento", 
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
            
        if (confirm != JOptionPane.YES_OPTION) return;

        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        progressBar.setString("Svuotamento in corso...");
        emptyButton.setEnabled(false);
        log("Inizio operazione svuotamento cestino...");
        isCancelled = false;

        SwingWorker<Void, String> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                cmisService.emptyTrashcan(new CmisService.ProgressCallback() {
                    @Override
                    public void onProgress(String message) {
                        publish(message);
                    }
                    @Override
                    public boolean isCancelled() {
                        return isCancelled;
                    }
                });
                return null;
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                if (!chunks.isEmpty()) {
                    String msg = chunks.get(chunks.size() - 1);
                    progressBar.setString(msg);
                    log(msg);
                }
            }

            @Override
            protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(App.this, "Cestino svuotato con successo!");
                    sizeLabel.setText("Dimensione Cestino: 0 B");
                    log("Cestino svuotato con successo.");
                } catch (Exception ex) {
                    ex.printStackTrace();
                    log("Errore durante lo svuotamento: " + ex.getCause().getMessage());
                    JOptionPane.showMessageDialog(App.this, "Errore svuotamento: " + ex.getCause().getMessage(), "Errore", JOptionPane.ERROR_MESSAGE);
                } finally {
                    progressBar.setVisible(false);
                    emptyButton.setEnabled(true);
                }
            }
        };
        worker.execute();
    }

    private void setLoading(boolean loading, String message) {
        connectButton.setEnabled(!loading);
        urlField.setEnabled(!loading);
        userField.setEnabled(!loading);
        passField.setEnabled(!loading);
        if (loading) {
            progressBar.setVisible(true);
            progressBar.setIndeterminate(true);
            progressBar.setString(message);
        } else {
            progressBar.setVisible(false);
        }
    }

    private String formatSize(long v) {
        if (v < 1024) return v + " B";
        int z = (63 - Long.numberOfLeadingZeros(v)) / 10;
        return String.format("%.1f %sB", (double)v / (1L << (z*10)), " KMGTPE".charAt(z));
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception ex) {
            System.err.println("Failed to initialize LaF");
        }

        SwingUtilities.invokeLater(() -> {
            new App().setVisible(true);
        });
    }
}

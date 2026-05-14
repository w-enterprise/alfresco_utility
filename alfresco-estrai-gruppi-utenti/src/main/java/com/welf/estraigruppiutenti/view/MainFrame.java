package com.welf.estraigruppiutenti.view;

import com.formdev.flatlaf.FlatLightLaf;
import com.welf.estraigruppiutenti.model.ConnectionConfig;
import com.welf.estraigruppiutenti.model.GroupInfo;
import com.welf.estraigruppiutenti.util.GuiLogAppender;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.event.TableModelEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Finestra principale dell'applicazione.
 */
public class MainFrame extends JFrame {
    private final GroupsTableModel tableModel = new GroupsTableModel();
    private final JTable groupsTable = new JTable(tableModel);
    private final SelectAllHeaderRenderer selectAllHeaderRenderer = new SelectAllHeaderRenderer();

    private final JTextField urlField = new JTextField();
    private final JTextField usernameField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final JButton connectButton = new JButton("Connetti");

    private final JButton refreshButton = new JButton("Aggiorna");
    private final JButton exportButton = new JButton("Esporta");
    private final JButton stopButton = new JButton("Interrompi");
    private final JProgressBar progressBar = new JProgressBar();
    private final JLabel statusLabel = new JLabel(" ");
    private final JTextArea logArea = new JTextArea();
    private boolean exporting;
    private boolean controlsEnabled = true;
    private boolean connectionControlsEnabled = true;

    /**
     * Crea la finestra principale.
     */
    public MainFrame() {
        super("Alfresco - Estrai gruppi/utenti");
        setupLookAndFeel();
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 700));
        setLayout(new BorderLayout());

        add(buildTopPanel(), BorderLayout.NORTH);

        groupsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        groupsTable.getTableHeader().setReorderingAllowed(false);
        groupsTable.setShowVerticalLines(false);
        groupsTable.setIntercellSpacing(new Dimension(0, 1));
        
        // Font più grandi per la tabella
        Font tableFont = new Font("Segoe UI", Font.PLAIN, 16);
        groupsTable.setFont(tableFont);
        groupsTable.setRowHeight(32);
        groupsTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 16));

        // Configurazione Colonne
        TableColumn selectCol = groupsTable.getColumnModel().getColumn(0);
        selectCol.setMaxWidth(45);
        selectCol.setMinWidth(45);
        selectCol.setPreferredWidth(45);
        selectCol.setHeaderRenderer(selectAllHeaderRenderer);

        // Colonna Utenti (indice 3): Larghezza fissa e allineamento centrale
        TableColumn usersCol = groupsTable.getColumnModel().getColumn(3);
        usersCol.setMinWidth(80);
        usersCol.setMaxWidth(100);
        usersCol.setPreferredWidth(90);
        
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        usersCol.setCellRenderer(centerRenderer);

        groupsTable.getTableHeader().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int viewColumn = groupsTable.columnAtPoint(e.getPoint());
                int modelColumn = groupsTable.convertColumnIndexToModel(viewColumn);
                if (modelColumn == 0) {
                    boolean newValue = !tableModel.areAllSelected();
                    tableModel.setAllSelected(newValue);
                    selectAllHeaderRenderer.setSelected(newValue);
                    groupsTable.getTableHeader().repaint();
                    updateExportButtonState();
                }
            }
        });

        tableModel.addTableModelListener(e -> {
            if (e.getType() == TableModelEvent.UPDATE) {
                selectAllHeaderRenderer.setSelected(tableModel.areAllSelected());
                groupsTable.getTableHeader().repaint();
                updateExportButtonState();
            }
        });

        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        logArea.setBackground(new Color(250, 250, 250));
        
        JScrollPane tableScroll = new JScrollPane(groupsTable);
        tableScroll.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Gruppi Repository"));

        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Log Attività"));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScroll, logScroll);
        split.setResizeWeight(0.7);
        split.setDividerSize(10);
        add(split, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        progressBar.setStringPainted(true);
        progressBar.setString(" ");
        bottom.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        bottom.add(progressBar, BorderLayout.CENTER);
        
        statusLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        bottom.add(statusLabel, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);

        exportButton.setEnabled(false);
        refreshButton.setEnabled(false);
        stopButton.setEnabled(false);
        progressBar.setVisible(false);

        GuiLogAppender.setLogConsumer(this::appendLog);
        pack();
        setLocationRelativeTo(null);
    }

    private void setupLookAndFeel() {
        try {
            FlatLightLaf.setup();
            UIManager.put("Button.arc", 8);
            UIManager.put("Component.arc", 8);
            UIManager.put("TextComponent.arc", 8);
            UIManager.put("ProgressBar.arc", 8);
        } catch (Exception ignored) {}
    }

    /**
     * Imposta i campi di connessione.
     *
     * @param config configurazione.
     */
    public void setConnectionConfig(ConnectionConfig config) {
        if (config == null) {
            return;
        }
        urlField.setText(config.getUrl());
        usernameField.setText(config.getUsername());
        passwordField.setText(config.getPassword());
    }

    /**
     * Legge la configurazione dai campi di connessione.
     *
     * @return configurazione inserita.
     */
    public ConnectionConfig getConnectionConfig() {
        return new ConnectionConfig(
                urlField.getText().trim(),
                usernameField.getText().trim(),
                new String(passwordField.getPassword())
        );
    }

    /**
     * Abilita/disabilita i controlli di connessione (campi + pulsante).
     *
     * @param enabled true per abilitare.
     */
    public void setConnectionControlsEnabled(boolean enabled) {
        this.connectionControlsEnabled = enabled;
        urlField.setEnabled(enabled);
        usernameField.setEnabled(enabled);
        passwordField.setEnabled(enabled);
        connectButton.setEnabled(enabled);
    }

    /**
     * Imposta la lista dei gruppi da visualizzare.
     *
     * @param groups gruppi.
     */
    public void setGroups(List<GroupInfo> groups) {
        tableModel.setGroups(groups);
        selectAllHeaderRenderer.setSelected(false);
        groupsTable.getTableHeader().repaint();
        updateExportButtonState();
    }

    /**
     * @return gruppi selezionati.
     */
    public List<GroupInfo> getSelectedGroups() {
        return tableModel.getSelectedGroups();
    }

    /**
     * Abilita o disabilita i controlli.
     *
     * @param enabled true per abilitare.
     */
    public void setControlsEnabled(boolean enabled) {
        this.controlsEnabled = enabled;
        refreshButton.setEnabled(enabled);
        updateExportButtonState();
    }

    /**
     * Imposta lo stato di export in corso.
     *
     * @param exporting true se export in corso.
     */
    public void setExporting(boolean exporting) {
        this.exporting = exporting;
        stopButton.setEnabled(exporting);
        refreshButton.setEnabled(!exporting);
        updateExportButtonState();
    }

    /**
     * Imposta l'abilitazione del pulsante Esporta in base alla presenza di gruppi.
     *
     * @param enabled true per abilitare.
     */
    public void setExportEnabled(boolean enabled) {
        updateExportButtonState();
    }

    /**
     * Mostra o nasconde la progress bar.
     *
     * @param visible true per mostrare.
     */
    public void setProgressVisible(boolean visible) {
        progressBar.setVisible(visible);
    }

    /**
     * Imposta progress bar indeterminata e messaggio.
     *
     * @param message messaggio.
     */
    public void setIndeterminateProgress(String message) {
        progressBar.setIndeterminate(true);
        progressBar.setString(message == null ? " " : message);
    }

    /**
     * Imposta il messaggio di stato.
     *
     * @param message messaggio.
     */
    public void setStatus(String message) {
        statusLabel.setText(message == null ? " " : message);
    }

    /**
     * Mostra un messaggio di errore.
     *
     * @param title titolo.
     * @param message messaggio.
     */
    public void showError(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
    }

    /**
     * @return pulsante Aggiorna.
     */
    public JButton getRefreshButton() {
        return refreshButton;
    }

    /**
     * @return pulsante Connetti.
     */
    public JButton getConnectButton() {
        return connectButton;
    }

    /**
     * @return pulsante Esporta.
     */
    public JButton getExportButton() {
        return exportButton;
    }

    /**
     * @return pulsante Interrompi.
     */
    public JButton getStopButton() {
        return stopButton;
    }

    private void updateExportButtonState() {
        boolean canExport = controlsEnabled && !exporting && tableModel.getRowCount() > 0 && !tableModel.getSelectedGroups().isEmpty();
        exportButton.setEnabled(canExport);
    }

    private void appendLog(String text) {
        if (SwingUtilities.isEventDispatchThread()) {
            logArea.append(text);
            logArea.setCaretPosition(logArea.getDocument().getLength());
        } else {
            SwingUtilities.invokeLater(() -> appendLog(text));
        }
    }

    private JPanel buildTopPanel() {
        JPanel container = new JPanel(new BorderLayout());

        JPanel connectionPanel = new JPanel(new GridBagLayout());
        connectionPanel.setBorder(BorderFactory.createTitledBorder("Connessione"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        connectionPanel.add(new JLabel("URL CMIS:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1;
        connectionPanel.add(urlField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        connectionPanel.add(new JLabel("Username:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 0.5;
        connectionPanel.add(usernameField, gbc);

        gbc.gridx = 2;
        gbc.gridy = 1;
        gbc.weightx = 0;
        connectionPanel.add(new JLabel("Password:"), gbc);

        gbc.gridx = 3;
        gbc.gridy = 1;
        gbc.weightx = 0.5;
        connectionPanel.add(passwordField, gbc);

        gbc.gridx = 4;
        gbc.gridy = 1;
        gbc.weightx = 0;
        connectionPanel.add(connectButton, gbc);

        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actionsPanel.add(refreshButton);
        actionsPanel.add(exportButton);
        actionsPanel.add(stopButton);

        container.add(connectionPanel, BorderLayout.CENTER);
        container.add(actionsPanel, BorderLayout.SOUTH);

        setConnectionControlsEnabled(connectionControlsEnabled);
        return container;
    }
}

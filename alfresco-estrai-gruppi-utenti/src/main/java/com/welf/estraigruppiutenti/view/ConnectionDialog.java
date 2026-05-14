package com.welf.estraigruppiutenti.view;

import com.welf.estraigruppiutenti.model.ConnectionConfig;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Dialogo modale per l'inserimento dei parametri di connessione.
 */
public class ConnectionDialog extends JDialog {
    /**
     * Callback invocata quando l'utente preme "Connetti".
     */
    public interface ConnectListener {
        /**
         * Esegue la connessione.
         *
         * @param config configurazione inserita.
         * @throws Exception in caso di errore: il messaggio viene mostrato all'utente.
         */
        void onConnect(ConnectionConfig config) throws Exception;
    }

    private final JTextField urlField = new JTextField();
    private final JTextField userField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private boolean connected;
    private ConnectListener connectListener;

    /**
     * Crea il dialogo.
     *
     * @param owner frame owner.
     */
    public ConnectionDialog(Frame owner) {
        super(owner, "Connessione Alfresco", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(620, 240));
        setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0;

        gbc.gridx = 0;
        gbc.gridy = 0;
        form.add(new JLabel("URL repository (CMIS):"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1;
        form.add(urlField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        form.add(new JLabel("Username:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 1;
        form.add(userField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        form.add(new JLabel("Password:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.weightx = 1;
        form.add(passwordField, gbc);

        add(form, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton connectButton = new JButton("Connetti");
        JButton cancelButton = new JButton("Annulla");
        buttons.add(connectButton);
        buttons.add(cancelButton);
        add(buttons, BorderLayout.SOUTH);

        connectButton.addActionListener(e -> attemptConnect());
        cancelButton.addActionListener(e -> {
            connected = false;
            dispose();
        });

        getRootPane().setDefaultButton(connectButton);
        pack();
        setLocationRelativeTo(owner);
    }

    /**
     * Imposta il listener di connessione.
     *
     * @param listener listener.
     */
    public void setConnectListener(ConnectListener listener) {
        this.connectListener = listener;
    }

    /**
     * Precompila i campi con i valori salvati.
     *
     * @param config configurazione.
     */
    public void setInitialConfig(ConnectionConfig config) {
        if (config == null) {
            return;
        }
        urlField.setText(config.getUrl());
        userField.setText(config.getUsername());
        passwordField.setText(config.getPassword());
    }

    /**
     * Mostra il dialogo in modo modale.
     *
     * @return true se la connessione è andata a buon fine.
     */
    public boolean showDialog() {
        setVisible(true);
        return connected;
    }

    private void attemptConnect() {
        if (connectListener == null) {
            JOptionPane.showMessageDialog(this, "Listener di connessione non configurato.", "Errore", JOptionPane.ERROR_MESSAGE);
            return;
        }
        ConnectionConfig config = new ConnectionConfig(
                urlField.getText().trim(),
                userField.getText().trim(),
                new String(passwordField.getPassword())
        );
        try {
            connectListener.onConnect(config);
            connected = true;
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Connessione fallita", JOptionPane.ERROR_MESSAGE);
        }
    }
}

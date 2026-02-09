package com.welf.estrainodi;

import com.welf.estrainodi.controller.AppController;
import com.welf.estrainodi.model.CmisModel;
import com.welf.estrainodi.view.AppView;

import javax.swing.*;

/**
 * Punto di ingresso dell'applicazione.
 */
public class Main {
    public static void main(String[] args) {
        // Esegue la creazione della GUI nell'Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            // Inizializza il Look and Feel del sistema
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                // Fallback al default se fallisce
                e.printStackTrace();
            }

            // Istanziazione MVC
            CmisModel model = new CmisModel();
            AppView view = new AppView();
            new AppController(view, model);

            // Mostra la finestra
            view.setVisible(true);
        });
    }
}

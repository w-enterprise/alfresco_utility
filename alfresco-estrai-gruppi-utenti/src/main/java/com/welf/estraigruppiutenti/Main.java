package com.welf.estraigruppiutenti;

import com.welf.estraigruppiutenti.controller.AppController;
import com.welf.estraigruppiutenti.view.MainFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Entry-point dell'applicazione desktop.
 */
public class Main {
    /**
     * Avvia l'applicazione.
     *
     * @param args argomenti.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            MainFrame frame = new MainFrame();
            AppController controller = new AppController(frame);
            controller.start();
        });
    }
}

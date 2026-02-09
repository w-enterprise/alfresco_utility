package it.welf.alfresco.tool;

import it.welf.alfresco.tool.ui.MainFrame;

import javax.swing.*;

public class App {
    public static void main(String[] args) {
        try {
            // Set System Look and Feel
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}

package com.duellite;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import com.duellite.ui.MainFrame;

public class App {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel"); } catch (Exception ignored) {}
            new MainFrame().setVisible(true);
        });
    }
}

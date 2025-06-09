package org.processadorcsv.viewmodel;

import javax.swing.*;

public class CarregadorIndices {
    private static JDialog dialog;

    public static void show(String mensagem) {
        if (dialog != null && dialog.isVisible()) return;

        JFrame frame = new JFrame();
        dialog = new JDialog(frame, "Aguarde", true);
        JPanel panel = new JPanel();
        panel.add(new JLabel(mensagem));
        panel.add(new JProgressBar() {{
            setIndeterminate(true);
        }});
        dialog.getContentPane().add(panel);
        dialog.setSize(300, 100);
        dialog.setLocationRelativeTo(null);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.setVisible(true);
    }

    public static void hide() {
        if (dialog != null) {
            dialog.setVisible(false);
            dialog.dispose();
        }
    }
}

package org.processadorcsv.viewmodel;

import javax.swing.*;
import java.awt.*;

public class BatchSizeDialog extends JDialog {
    private int batchSize = -1;
    public BatchSizeDialog(JFrame parent) {
        super(parent, "Definir Batch Size", true);
        setSize(300, 150);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());
        JLabel label = new JLabel("Informe o tamanho do batch (linhas por vez):");
        JTextField inputField = new JTextField("");
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(label, BorderLayout.NORTH);
        inputPanel.add(inputField, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.CENTER);
        JButton okButton = new JButton("Confirmar");
        okButton.addActionListener(e -> {
            try {
                int value = Integer.parseInt(inputField.getText());
                if (value > 0) {
                    batchSize = value;
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(this, "Valor deve ser maior que 0.");
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Insira um número válido.");
            }
        });
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(okButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }
    public int getBatchSize() {
        return batchSize;
    }
}
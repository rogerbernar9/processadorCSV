package org.processadorcsv.viewmodel;

import javax.swing.*;
import java.awt.*;
import java.util.Vector;

public class EdicaoEmMassaDialog extends JDialog {
    private JComboBox<String> colunaComboBox;
    private JTextField novoValorField;
    private boolean confirmado = false;
    public EdicaoEmMassaDialog(Frame owner, Vector<String> colunas) {
        super(owner, "Edição em Massa", true);
        setLayout(new BorderLayout());
        setSize(350, 180);
        setLocationRelativeTo(owner);
        JPanel formPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));
        formPanel.add(new JLabel("Coluna:"));
        colunaComboBox = new JComboBox<>(colunas);
        formPanel.add(colunaComboBox);
        formPanel.add(new JLabel("Novo Valor:"));
        novoValorField = new JTextField();
        formPanel.add(novoValorField);
        add(formPanel, BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel();
        JButton confirmarBtn = new JButton("Aplicar");
        JButton cancelarBtn = new JButton("Cancelar");
        buttonPanel.add(confirmarBtn);
        buttonPanel.add(cancelarBtn);
        confirmarBtn.addActionListener(e -> {
            confirmado = true;
            dispose();
        });
        cancelarBtn.addActionListener(e -> dispose());
        add(buttonPanel, BorderLayout.SOUTH);
    }
    public String getColunaSelecionada() {
        return (String) colunaComboBox.getSelectedItem();
    }
    public String getNovoValor() {
        return novoValorField.getText();
    }
    public boolean isConfirmado() {
        return confirmado;
    }
}
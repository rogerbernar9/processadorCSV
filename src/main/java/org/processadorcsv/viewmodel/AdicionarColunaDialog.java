package org.processadorcsv.viewmodel;

import org.processadorcsv.jdbd.db.DatabaseUtil;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
public class AdicionarColunaDialog extends JDialog {
    private JTextField nomeColunaField;
    private JComboBox<String> tipoComboBox;
    private boolean colunaCriada = false;
    public AdicionarColunaDialog(JFrame parent) {
        super(parent, "Adicionar Nova Coluna", true);
        setLayout(new BorderLayout());
        setSize(400, 150);
        setLocationRelativeTo(parent);
        nomeColunaField = new JTextField();
        String[] tipos = {"TEXT", "INTEGER", "REAL", "BLOB", "DATE"};
        tipoComboBox = new JComboBox<>(tipos);
        JPanel inputPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        inputPanel.add(new JLabel("Nome da nova coluna:"));
        inputPanel.add(nomeColunaField);
        inputPanel.add(new JLabel("Tipo da nova coluna:"));
        inputPanel.add(tipoComboBox);
        add(inputPanel, BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("Criar");
        JButton cancelButton = new JButton("Cancelar");
        buttonPanel.add(cancelButton);
        buttonPanel.add(okButton);
        add(buttonPanel, BorderLayout.SOUTH);
        okButton.addActionListener(e -> criarColuna());
        cancelButton.addActionListener(e -> dispose());
    }
    private void criarColuna() {
        String nomeColuna = nomeColunaField.getText().trim();
        String tipoColuna = (String) tipoComboBox.getSelectedItem();
        if (nomeColuna.isEmpty()) {
            JOptionPane.showMessageDialog(this, "O nome da coluna não pode estar vazio.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DatabaseUtil.getPath());
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("ALTER TABLE csv_data ADD COLUMN \"" + nomeColuna + "\" " + tipoColuna);
            colunaCriada = true;
            JOptionPane.showMessageDialog(this, "Coluna adicionada com sucesso!");
            dispose();
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao adicionar coluna:\n" + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
    public boolean isColunaCriada() {
        return colunaCriada;
    }
}

package org.processadorcsv.viewmodel;
import org.processadorcsv.jdbd.db.DatabaseUtil;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;

public class RenomearColunas extends JDialog {
    private final Vector<String> columnNames;
    private final List<JTextField> novosNomesCampos;
    public RenomearColunas(JFrame parent, Vector<String> columnNames) {
        super(parent, "Renomear Colunas", true);
        this.columnNames = columnNames;
        this.novosNomesCampos = new java.util.ArrayList<>();
        setSize(600, 400);
        setLayout(new BorderLayout());
        JPanel camposPanel = new JPanel(new GridLayout(0, 2, 10, 10));
        for (String nomeColuna : columnNames) {
            camposPanel.add(new JLabel("Coluna: " + nomeColuna));
            JTextField campo = new JTextField(nomeColuna);
            novosNomesCampos.add(campo);
            if(nomeColuna.equalsIgnoreCase("id")) {
                campo.setEditable(false);
            }
            camposPanel.add(campo);
        }
        JButton aplicarButton = new JButton("Aplicar Renomeações");
        aplicarButton.addActionListener(e -> aplicarRenomeacoes());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(aplicarButton);
        add(new JScrollPane(camposPanel), BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        setLocationRelativeTo(parent);
    }
    private void aplicarRenomeacoes() {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DatabaseUtil.getPath())) {
            for (int i = 0; i < columnNames.size(); i++) {
                String nomeAtual = columnNames.get(i);
                String novoNome = novosNomesCampos.get(i).getText().trim();
                if (!novoNome.equals(nomeAtual) && !novoNome.isEmpty()) {
                    String sql = String.format("ALTER TABLE csv_data RENAME COLUMN \"%s\" TO \"%s\";", nomeAtual, novoNome);
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute(sql);
                    }
                }
            }
            JOptionPane.showMessageDialog(this, "Colunas renomeadas com sucesso.");
            dispose();
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao renomear colunas.", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

}

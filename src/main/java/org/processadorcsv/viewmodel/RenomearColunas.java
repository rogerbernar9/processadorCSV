package org.processadorcsv.viewmodel;
import org.processadorcsv.jdbd.db.DatabaseUtil;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

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
            conn.setAutoCommit(false);

            Set<String> indicesExistentes = new HashSet<>();
            try (Statement stmt = conn.createStatement();
                 var rs = stmt.executeQuery("PRAGMA index_list(csv_data)")) {
                while (rs.next()) {
                    String nomeIndice = rs.getString("name");
                    if (nomeIndice != null && nomeIndice.startsWith("idx_")) {
                        indicesExistentes.add(nomeIndice);
                    }
                }
            }

            List<String> colunasAntigas = new ArrayList<>();
            List<String> colunasNovas = new ArrayList<>();
            List<String> colunasComIndice = new ArrayList<>();

            for (int i = 0; i < columnNames.size(); i++) {
                String nomeAtual = columnNames.get(i);
                String novoNome = novosNomesCampos.get(i).getText().trim();

                if (!novoNome.equals(nomeAtual) && !novoNome.isEmpty()) {
                    colunasAntigas.add(nomeAtual);
                    colunasNovas.add(novoNome);

                    String indiceAntigo = "idx_" + nomeAtual;
                    if (indicesExistentes.contains(indiceAntigo)) {
                        colunasComIndice.add(nomeAtual);
                    }
                }
            }

            if (colunasAntigas.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Nenhuma coluna foi renomeada.");
                return;
            }

            try (Statement stmt = conn.createStatement()) {
                for (int i = 0; i < colunasAntigas.size(); i++) {
                    String sql = String.format("ALTER TABLE csv_data RENAME COLUMN \"%s\" TO \"%s\";",
                            colunasAntigas.get(i), colunasNovas.get(i));
                    stmt.execute(sql);
                }
            }

            try (Statement stmt = conn.createStatement()) {
                for (String colunaAntiga : colunasComIndice) {
                    String indexName = "idx_" + colunaAntiga;
                    stmt.executeUpdate("DROP INDEX IF EXISTS " + indexName);
                }
            }

            if (!colunasComIndice.isEmpty()) {
                try (Statement stmt = conn.createStatement()) {
                    for (int i = 0; i < colunasAntigas.size(); i++) {
                        String colunaAntiga = colunasAntigas.get(i);
                        String colunaNova = colunasNovas.get(i);

                        if (colunasComIndice.contains(colunaAntiga)) {
                            String novoIndice = "idx_" + colunaNova;
                            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS " + novoIndice +
                                    " ON csv_data(\"" + colunaNova + "\")");
                        }
                    }
                }
            }

            conn.commit();
            JOptionPane.showMessageDialog(this, "Colunas renomeadas com sucesso." +
                    (colunasComIndice.isEmpty() ? "" : " Índices atualizados."));
            dispose();
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao renomear colunas.", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

}

package org.processadorcsv.viewmodel;

import org.processadorcsv.jdbd.db.DatabaseUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.FileWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.Vector;
import java.util.List;

public class VisualizadorDados extends JFrame {

    private final DefaultTableModel tableModel = new DefaultTableModel();
    private final JTable table = new JTable(tableModel);
    private final JTextField filterField1 = new JTextField(21);
    private final JTextField filterField2 = new JTextField(21);
    private final JTextField filterField3 = new JTextField(21);
    private final JButton previousButton = new JButton("Anterior");
    private final JButton nextButton = new JButton("Próximo");
    private final JButton telaAnterior = new JButton("Tela Principal");
    private int currentPage = 0;
    private final int pageSize = 100;
    private final Vector<String> columnNames = new Vector<>();
    JButton insertButton = new JButton("Inserir");
    JButton editButton = new JButton("Editar");
    JButton deleteButton = new JButton("Excluir");
    JButton exportButton = new JButton("Exportar CSV");
    JButton exportSqlButton = new JButton("Exportar SQL");
    JCheckBox[] sanitizeChecks = new JCheckBox[columnNames.size()];


    public VisualizadorDados() {
        setTitle("CSV Processador");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1600, 600);
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton loadButton = new JButton("Carregar dados");
        topPanel.add(new JLabel("Filtro 1 :"));
        topPanel.add(filterField1);

        topPanel.add(new JLabel("Filtro 2 :"));
        topPanel.add(filterField2);

        topPanel.add(new JLabel("Filtro 3 :"));
        topPanel.add(filterField3);

        topPanel.add(loadButton);
        topPanel.add(telaAnterior);
        topPanel.add(insertButton);
        topPanel.add(editButton);
        topPanel.add(deleteButton);
        topPanel.add(exportButton);

        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel bottomContainer = new JPanel(new FlowLayout(FlowLayout.CENTER));

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JPanel bottomPanel2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottomPanel2.add(telaAnterior);

        bottomPanel.add(previousButton);
        bottomPanel.add(nextButton);

        bottomContainer.add(bottomPanel2);
        bottomContainer.add(bottomPanel);

        add(bottomContainer, BorderLayout.SOUTH);

        loadButton.addActionListener(e -> {
            currentPage = 0;
            loadData();
        });

        previousButton.addActionListener(e -> {
            if (currentPage > 0) {
                currentPage--;
                loadData();
            }
        });

        nextButton.addActionListener(e -> {
            currentPage++;
            loadData();
        });

        telaAnterior.addActionListener(e -> {
            new CsvReader().setVisible(true);
            this.setVisible(false);
        });

        insertButton.addActionListener(e -> abrirDialogDeInsercao());

        editButton.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow >= 0) abrirDialogDeEdicao(selectedRow);
            else JOptionPane.showMessageDialog(this, "Selecione uma linha para editar.");
        });
        deleteButton.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow >= 0) excluirRegistroSelecionado(selectedRow);
            else JOptionPane.showMessageDialog(this, "Selecione uma linha para excluir.");
        });
        exportButton.addActionListener(e -> exportarParaCSV());

        topPanel.add(exportSqlButton);
        exportSqlButton.addActionListener(e -> abrirDialogExportarSQL());


        loadColumnNames();
    }

    private void loadColumnNames() {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:"+ DatabaseUtil.getPath())) {
            Statement stmt = conn.createStatement();
            ResultSet rsColumns = stmt.executeQuery("PRAGMA table_info(csv_data)");
            while (rsColumns.next()) {
                String name = rsColumns.getString("name");
                if (!name.equalsIgnoreCase("id")) {
                    columnNames.add(name);
                }
            }
            rsColumns.close();
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao carregar nomes das colunas.", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadData() {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:"+ DatabaseUtil.getPath())) {
            Statement stmt = conn.createStatement();
            if (columnNames.isEmpty()) {

                loadColumnNames();
                if (columnNames.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Não foram encontradas colunas para exibir.", "Erro", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
            String whereClause = buildWhereClause();
            String selectColumns = String.join(", ", columnNames);
            String sql = String.format("SELECT %s FROM csv_data %s LIMIT %d OFFSET %d",
                    selectColumns,
                    whereClause,
                    pageSize,
                    currentPage * pageSize);
//            System.out.println("debug " + sql);
            ResultSet rs = stmt.executeQuery(sql);
            tableModel.setRowCount(0);
            tableModel.setColumnIdentifiers(columnNames);
            while (rs.next()) {
                Vector<String> row = new Vector<>();
                for (String col : columnNames) {
                    row.add(rs.getString(col));
                }
                tableModel.addRow(row);
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao carregar dados.", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String buildWhereClause() {
        StringBuilder whereClause = new StringBuilder();
        String filter1 = filterField1.getText().trim();
        String filter2 = filterField2.getText().trim();
        String filter3 = filterField3.getText().trim();
        boolean firstCondition = true;
        if (!filter1.isEmpty()) {
            whereClause.append("(");
            for (int i = 0; i < columnNames.size(); i++) {
                whereClause.append(columnNames.get(i)).append(" LIKE '%").append(filter1).append("%'");
                if (i < columnNames.size() - 1) {
                    whereClause.append(" OR ");
                }
            }
            whereClause.append(")");
            firstCondition = false;
        }
        if (!filter2.isEmpty()) {
            if (!firstCondition) {
                whereClause.append(" AND ");
            }
            whereClause.append("(");
            for (int i = 0; i < columnNames.size(); i++) {
                whereClause.append(columnNames.get(i)).append(" LIKE '%").append(filter2).append("%'");
                if (i < columnNames.size() - 1) {
                    whereClause.append(" OR ");
                }
            }
            whereClause.append(")");
            firstCondition = false;
        }
        if (!filter3.isEmpty()) {
            if (!firstCondition) {
                whereClause.append(" AND ");
            }
            whereClause.append("(");
            for (int i = 0; i < columnNames.size(); i++) {
                whereClause.append(columnNames.get(i)).append(" LIKE '%").append(filter3).append("%'");
                if (i < columnNames.size() - 1) {
                    whereClause.append(" OR ");
                }
            }
            whereClause.append(")");
        }
        if (whereClause.length() > 0) {
            return "WHERE " + whereClause.toString();
        } else {
            return "";
        }
    }

    private void abrirDialogDeInsercao() {
        JDialog dialog = new JDialog(this, "Inserir Registro", true);
        dialog.setLayout(new GridLayout(columnNames.size() + 1, 2));
        JTextField[] fields = new JTextField[columnNames.size()];
        for (int i = 0; i < columnNames.size(); i++) {
            dialog.add(new JLabel(columnNames.get(i)));
            fields[i] = new JTextField();
            dialog.add(fields[i]);
        }
        JButton salvar = new JButton("Salvar");
        dialog.add(salvar);
        salvar.addActionListener(e -> {
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DatabaseUtil.getPath())) {
                StringBuilder sql = new StringBuilder("INSERT INTO csv_data (");
                sql.append(String.join(", ", columnNames));
                sql.append(") VALUES (");
                sql.append("?, ".repeat(columnNames.size()));
                sql.setLength(sql.length() - 2);
                sql.append(")");
                PreparedStatement ps = conn.prepareStatement(sql.toString());
                for (int i = 0; i < fields.length; i++) ps.setString(i + 1, fields[i].getText());
                ps.executeUpdate();
                dialog.dispose();
                loadData();
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Erro ao inserir registro");
            }
        });
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void abrirDialogDeEdicao(int rowIndex) {
        JDialog dialog = new JDialog(this, "Editar Registro", true);
        dialog.setLayout(new GridLayout(columnNames.size() + 1, 2));
        JTextField[] fields = new JTextField[columnNames.size()];
        for (int i = 0; i < columnNames.size(); i++) {
            dialog.add(new JLabel(columnNames.get(i)));
            fields[i] = new JTextField((String) tableModel.getValueAt(rowIndex, i));
            dialog.add(fields[i]);
        }
        JButton salvar = new JButton("Salvar");
        dialog.add(salvar);
        salvar.addActionListener(e -> {
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DatabaseUtil.getPath())) {
                StringBuilder sql = new StringBuilder("UPDATE csv_data SET ");
                for (String col : columnNames) sql.append(col).append(" = ?, ");
                sql.setLength(sql.length() - 2);
                sql.append(" WHERE rowid = ?"); // rowid é suportado por SQLite se você não tiver id
                PreparedStatement ps = conn.prepareStatement(sql.toString());
                for (int i = 0; i < fields.length; i++) ps.setString(i + 1, fields[i].getText());
                ps.setInt(columnNames.size() + 1, rowIndex + 1 + currentPage * pageSize);
                ps.executeUpdate();
                dialog.dispose();
                loadData();
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Erro ao editar registro");
            }
        });
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void excluirRegistroSelecionado(int rowIndex) {
        int confirm = JOptionPane.showConfirmDialog(this, "Tem certeza que deseja excluir?", "Confirmar", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DatabaseUtil.getPath())) {
                PreparedStatement ps = conn.prepareStatement("DELETE FROM csv_data WHERE rowid = ?");
                ps.setInt(1, rowIndex + 1 + currentPage * pageSize);
                ps.executeUpdate();
                loadData();
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Erro ao excluir registro");
            }
        }
    }

    private void exportarParaCSV() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DatabaseUtil.getPath());
                 Statement stmt = conn.createStatement();
                 FileWriter writer = new FileWriter(fileChooser.getSelectedFile())) {

                if (columnNames.isEmpty()) {
                    loadColumnNames();
                }

                /**
                 * ignora exportação dos nomes das colunas
                for (int i = 0; i < columnNames.size(); i++) {
                    writer.write(columnNames.get(i));
                    if (i < columnNames.size() - 1) writer.write(",");
                }
                writer.write("\n");
                 **/

                String sql = "SELECT " + String.join(", ", columnNames) + " FROM csv_data";
                ResultSet rs = stmt.executeQuery(sql);

                while (rs.next()) {
                    for (int i = 0; i < columnNames.size(); i++) {
                        writer.write(rs.getString(columnNames.get(i)));
                        if (i < columnNames.size() - 1) writer.write(",");
                    }
                    writer.write("\n");
                }
                JOptionPane.showMessageDialog(this, "Exportado com sucesso.");
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Erro ao exportar CSV.");
            }
        }
    }

    private int countSelected(JCheckBox[] checks) {
        int count = 0;
        for (JCheckBox cb : checks) {
            if (cb.isSelected()) count++;
        }
        return count;
    }

    private void abrirDialogExportarSQL() {
        JDialog dialog = new JDialog(this, "Exportar SQL com personalização", true);
        dialog.setLayout(new BorderLayout());
        JPanel mainPanel = new JPanel(new BorderLayout());

        JPanel tableNamePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        tableNamePanel.add(new JLabel("Nome da Tabela:"));
        JTextField tableNameField = new JTextField("csv_data", 20);
        tableNamePanel.add(tableNameField);

        JPanel customPanel = new JPanel(new GridLayout(columnNames.size(),  5, 5,5));
        JTextField[] renamedFields = new JTextField[columnNames.size()];
        JComboBox<String>[] typeSelectors = new JComboBox[columnNames.size()];
        JCheckBox[] columnChecks = new JCheckBox[columnNames.size()];
        String[] tiposDados = {"VARCHAR", "INTEGER", "DATE", "BOOLEAN", "DOUBLE"};
        JCheckBox[] sanitizeChecks = new JCheckBox[columnNames.size()];
        for (int i = 0; i < columnNames.size(); i++) {
            String originalName = columnNames.get(i);
            columnChecks[i] = new JCheckBox();
            columnChecks[i].setSelected(true);
            renamedFields[i] = new JTextField(originalName);
            typeSelectors[i] = new JComboBox<>(tiposDados);
            sanitizeChecks[i] = new JCheckBox("Limpar especiais");

            customPanel.add(columnChecks[i]);
            customPanel.add(new JLabel("Coluna: " + originalName));
            customPanel.add(renamedFields[i]);
            customPanel.add(typeSelectors[i]);
            customPanel.add(sanitizeChecks[i]);

        }
        JButton exportarButton = new JButton("Exportar SQL");
        exportarButton.addActionListener(e -> {
            try {
                String tableName = tableNameField.getText().trim();
                if (tableName.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, "Informe um nome válido para a tabela.");
                    return;
                }
                StringBuilder sb = new StringBuilder();
                for (int row = 0; row < tableModel.getRowCount(); row++) {
                    sb.append("INSERT INTO ").append(tableName).append(" (");
                    // Adiciona nomes das colunas selecionadas
                    List<Integer> selectedIndexes = new ArrayList<>();
                    for (int i = 0; i < columnChecks.length; i++) {
                        if (columnChecks[i].isSelected()) {
                            selectedIndexes.add(i);
                            sb.append(renamedFields[i].getText());
                            if (selectedIndexes.size() < countSelected(columnChecks)) {
                                sb.append(", ");
                            }
                        }
                    }
                    sb.append(") VALUES (");
                    // Adiciona os valores correspondentes
                    for (int i = 0; i < selectedIndexes.size(); i++) {
                        int col = selectedIndexes.get(i);
                        String tipo = (String) typeSelectors[col].getSelectedItem();
                        Object valor = tableModel.getValueAt(row, col);
                        String valorFormatado;
                        if (valor == null) {
                            valorFormatado = "NULL";
                        } else if ("INTEGER".equals(tipo) || "DOUBLE".equals(tipo)) {
                            valorFormatado = valor.toString();
                        } else if ("BOOLEAN".equals(tipo)) {
                            valorFormatado = Boolean.parseBoolean(valor.toString()) ? "TRUE" : "FALSE";
                        } else {
                            String texto = valor.toString();
                            if (sanitizeChecks[col].isSelected()) {
                                texto = texto.replaceAll("[.,\\-*]", ""); // Remove caracteres especiais
                            }
                            valorFormatado = "'" + texto.replace("'", "''") + "'";
                        }
                        sb.append(valorFormatado);
                        if (i < selectedIndexes.size() - 1) sb.append(", ");
                    }
                    sb.append(");\n");
                }
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Salvar como SQL");
                int userSelection = fileChooser.showSaveDialog(this);
                if (userSelection == JFileChooser.APPROVE_OPTION) {
                    FileWriter writer = new FileWriter(fileChooser.getSelectedFile());
                    writer.write(sb.toString());
                    writer.close();
                    JOptionPane.showMessageDialog(this, "Arquivo SQL exportado com sucesso!");
                }
                dialog.dispose();
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Erro ao exportar SQL.");
            }
        });
        mainPanel.add(tableNamePanel, BorderLayout.NORTH);
        mainPanel.add(new JScrollPane(customPanel), BorderLayout.CENTER);
        dialog.add(mainPanel, BorderLayout.CENTER);
        dialog.add(exportarButton, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
    private void exportarDadosComoInsertSQL(String nomeTabela, String[] tipos) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Salvar como SQL");
        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection != JFileChooser.APPROVE_OPTION) return;
        try (FileWriter writer = new FileWriter(fileChooser.getSelectedFile())) {
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                StringBuilder sb = new StringBuilder();
                sb.append("INSERT INTO ").append(nomeTabela).append(" (")
                        .append(String.join(", ", columnNames)).append(") VALUES (");
                for (int j = 0; j < columnNames.size(); j++) {
                    String value = String.valueOf(tableModel.getValueAt(i, j));
                    if (tipos[j].toLowerCase().contains("int") || tipos[j].toLowerCase().contains("float")) {
                        sb.append(value.isEmpty() ? "NULL" : value);
                    } else {
                        sb.append("'").append(value.replace("'", "''")).append("'");
                    }
                    if (j < columnNames.size() - 1) sb.append(", ");
                }
                sb.append(");\n");
                writer.write(sb.toString());
            }
            writer.flush();
            JOptionPane.showMessageDialog(this, "Exportação SQL concluída.");
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao exportar para SQL.");
        }
    }

}

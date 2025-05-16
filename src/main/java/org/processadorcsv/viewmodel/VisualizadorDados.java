package org.processadorcsv.viewmodel;

import org.processadorcsv.jdbd.db.DatabaseUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.FileWriter;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Vector;
import java.util.List;

public class VisualizadorDados extends JFrame {

    private final JLabel totalRegistrosLabel = new JLabel("Total de registros: 0");
    private final DefaultTableModel tableModel = new DefaultTableModel();
    private final JTable table = new JTable(tableModel);
    private final JTextField filterField1 = new JTextField(21);
    private final JTextField filterField2 = new JTextField(21);
    private final JTextField filterField3 = new JTextField(21);
    private final JButton previousButton = new JButton("Anterior");
    private final JButton nextButton = new JButton("Próximo");
    private final JButton telaAnterior = new JButton("Tela Principal");
    private int currentPage = 0;
    private final int pageSize = 250;
    private final Vector<String> columnNames = new Vector<>();
    JButton insertButton = new JButton("Inserir");
    JButton editButton = new JButton("Editar");
    JButton deleteButton = new JButton("Excluir");
    JButton exportSqlButton = new JButton("Exportar SQL");
    JCheckBox[] sanitizeChecks = new JCheckBox[columnNames.size()];
    JCheckBox[] parseDateChecks = new JCheckBox[columnNames.size()];


    public VisualizadorDados() {
        setTitle("CSV Processador");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1600, 600);
        setLayout(new BorderLayout());

        JMenuBar menuBar = new JMenuBar();
        JMenu menuArquivo = new JMenu("Opções");
        JMenu menuExportacao = new JMenu("Exportação");

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

        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel bottomContainer = new JPanel(new FlowLayout(FlowLayout.CENTER));

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JPanel bottomPanel2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottomPanel2.add(totalRegistrosLabel);

        bottomPanel2.add(telaAnterior);

        bottomPanel.add(previousButton);
        bottomPanel.add(nextButton);

        bottomContainer.add(bottomPanel2);
        bottomContainer.add(bottomPanel);

        add(bottomContainer, BorderLayout.SOUTH);

        JMenuItem menuItemRenomearColumas = new JMenuItem("Renomear Colunas");
        JMenuItem menuItemAdicionarColuna = new JMenuItem("Adicionar Nova Coluna");

        JMenuItem menuItemExportarCSV = new JMenuItem("Exportar para CSV");
        JMenuItem menuItemExportarSQL = new JMenuItem("Exportar para SQL insert");
        JMenuItem menuItemEdicaoMassa = new JMenuItem("Edição em Massa");

        menuArquivo.add(menuItemRenomearColumas);
        menuArquivo.add(menuItemAdicionarColuna);
        menuArquivo.add(menuItemEdicaoMassa);

        menuExportacao.add(menuItemExportarCSV);
        menuExportacao.add(menuItemExportarSQL);

        // Adiciona o menu à barra de menu
        menuBar.add(menuArquivo);
        menuBar.add(menuExportacao);

        // Define a barra de menu na Janela
        setJMenuBar(menuBar);

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

        menuItemExportarCSV.addActionListener(e -> exportarParaCSV());

        menuItemExportarSQL.addActionListener(e -> abrirDialogExportarSQL());
        menuItemRenomearColumas.addActionListener(e -> {
            RenomearColunas dialog = new RenomearColunas(this, columnNames);
            dialog.setVisible(true);
            columnNames.clear();
            loadColumnNames();
            loadData();
        });

        menuItemAdicionarColuna.addActionListener(e -> {
            AdicionarColunaDialog dialog = new AdicionarColunaDialog(this);
            dialog.setVisible(true);
            if (dialog.isColunaCriada()) {
                columnNames.clear();
                loadColumnNames();
                loadData();
            }
        });

        menuItemEdicaoMassa.addActionListener(e -> {
            int[] linhasSelecionadas = table.getSelectedRows();
            if (linhasSelecionadas.length == 0) {
                JOptionPane.showMessageDialog(this, "Selecione ao menos uma linha para editar.");
                return;
            }
            EdicaoEmMassaDialog dialog = new EdicaoEmMassaDialog(this, columnNames);
            dialog.setVisible(true);
            if (dialog.isConfirmado()) {
                String coluna = dialog.getColunaSelecionada();
                String novoValor = dialog.getNovoValor();
                int colunaIndex = columnNames.indexOf(coluna);
                for (int row : linhasSelecionadas) {
                    tableModel.setValueAt(novoValor, row, colunaIndex);
                }
                atualizarDadosNoBancoEmMassa(linhasSelecionadas, coluna, novoValor);
            }
        });

        loadColumnNames();
        carregaQuantidadeDados();
    }

    private void loadColumnNames() {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:"+ DatabaseUtil.getPath())) {
            Statement stmt = conn.createStatement();
            ResultSet rsColumns = stmt.executeQuery("PRAGMA table_info(csv_data)");
            while (rsColumns.next()) {
                String name = rsColumns.getString("name");
                    columnNames.add(name);
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
        } finally {
            carregaQuantidadeDados();
        }
    }

    private void carregaQuantidadeDados() {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:"+ DatabaseUtil.getPath())) {
            Statement stmt = conn.createStatement();

            String countSql = "SELECT COUNT(*) FROM csv_data " + buildWhereClause();
            ResultSet countRs = stmt.executeQuery(countSql);
            if (countRs.next()) {
                int total = countRs.getInt(1);
                totalRegistrosLabel.setText("Total de registros: " + total);
            }
            countRs.close();
        } catch (SQLException e) {
            e.printStackTrace();
            totalRegistrosLabel.setText("Erro ao contar registros.");
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
            if (columnNames.get(i).equalsIgnoreCase("id")) {
                fields[i].setEditable(false);
            }
            dialog.add(fields[i]);
        }
        JButton salvar = new JButton("Salvar");
        dialog.add(salvar);
        salvar.addActionListener(e -> {
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DatabaseUtil.getPath())) {
                StringBuilder sql = new StringBuilder("UPDATE csv_data SET ");
                List<String> colunasAtualizaveis = new ArrayList<>();
                for (String col : columnNames) {
                    if (!col.equalsIgnoreCase("id")) {
                        sql.append(col).append(" = ?, ");
                        colunasAtualizaveis.add(col);
                    }
                }
                sql.setLength(sql.length() - 2);
                sql.append(" WHERE id = ?");
                PreparedStatement ps = conn.prepareStatement(sql.toString());
                int paramIndex = 1;
                for (int i = 0; i < columnNames.size(); i++) {
                    if (!columnNames.get(i).equalsIgnoreCase("id")) {
                        ps.setString(paramIndex++, fields[i].getText());
                    }
                }

                String idValue = null;
                for (int i = 0; i < columnNames.size(); i++) {
                    if (columnNames.get(i).equalsIgnoreCase("id")) {
                        idValue = fields[i].getText();
                        break;
                    }
                }
                ps.setString(paramIndex, idValue);
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

    private void abrirDialogExportarSQL() {
        JDialog dialog = new JDialog(this, "Exportar SQL com personalização", true);
        dialog.setLayout(new BorderLayout());
        JPanel mainPanel = new JPanel(new BorderLayout());
        JPanel tableNamePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        tableNamePanel.add(new JLabel("Nome da Tabela:"));
        JTextField tableNameField = new JTextField("csv_data", 20);
        tableNamePanel.add(tableNameField);
        // Componentes globais para o painel dinâmico
        JPanel customPanel = new JPanel(new GridLayout(0, 6, 5, 5));
        JScrollPane scrollPane = new JScrollPane(customPanel);
        // Declarar arrays com tamanho máximo (ajustável se preferir usar listas)
        final int MAX_COLUMNS = 100;
        JCheckBox[] columnChecks = new JCheckBox[MAX_COLUMNS];
        JTextField[] renamedFields = new JTextField[MAX_COLUMNS];
        JComboBox<String>[] typeSelectors = new JComboBox[MAX_COLUMNS];
        JCheckBox[] sanitizeChecks = new JCheckBox[MAX_COLUMNS];
        JCheckBox[] parseDateChecks = new JCheckBox[MAX_COLUMNS];
        String[] tiposDados = {"VARCHAR", "INTEGER", "DATE", "BOOLEAN", "DOUBLE"};
        // Listas para armazenar os dados e nomes de colunas do banco
        List<String> columnNames = new ArrayList<>();
        List<List<Object>> dadosTabela = new ArrayList<>();
        // Botão para carregar colunas do banco de dados
        JButton carregarColunasButton = new JButton("Carregar Colunas");
        carregarColunasButton.addActionListener(ev -> {
            columnNames.clear();
            dadosTabela.clear();
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DatabaseUtil.getPath());
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableNameField.getText().trim())) {
                ResultSetMetaData meta = rs.getMetaData();
                for (int i = 1; i <= meta.getColumnCount(); i++) {
                    columnNames.add(meta.getColumnName(i));
                }
                while (rs.next()) {
                    List<Object> linha = new ArrayList<>();
                    for (int i = 1; i <= columnNames.size(); i++) {
                        linha.add(rs.getObject(i));
                    }
                    dadosTabela.add(linha);
                }
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(dialog, "Erro ao carregar dados do banco.");
                return;
            }
            customPanel.removeAll();
            for (int i = 0; i < columnNames.size(); i++) {
                String originalName = columnNames.get(i);
                columnChecks[i] = new JCheckBox();
                columnChecks[i].setSelected(true);
                renamedFields[i] = new JTextField(originalName);
                typeSelectors[i] = new JComboBox<>(tiposDados);
                sanitizeChecks[i] = new JCheckBox("Limpar especiais");
                parseDateChecks[i] = new JCheckBox("Converter p/ Data");
                customPanel.add(columnChecks[i]);
                customPanel.add(new JLabel("Coluna: " + originalName));
                customPanel.add(renamedFields[i]);
                customPanel.add(typeSelectors[i]);
                customPanel.add(sanitizeChecks[i]);
                customPanel.add(parseDateChecks[i]);
            }
            customPanel.revalidate();
            customPanel.repaint();
        });
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(tableNamePanel, BorderLayout.WEST);
        topPanel.add(carregarColunasButton, BorderLayout.EAST);
        JButton exportarButton = new JButton("Exportar SQL");
        exportarButton.addActionListener(e -> {
            try {
                String tableName = tableNameField.getText().trim();
                if (tableName.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, "Informe um nome válido para a tabela.");
                    return;
                }
                StringBuilder sb = new StringBuilder();
                for (List<Object> row : dadosTabela) {
                    sb.append("INSERT INTO ").append(tableName).append(" (");
                    List<Integer> selectedIndexes = new ArrayList<>();
                    for (int i = 0; i < columnNames.size(); i++) {
                        if (columnChecks[i] != null && columnChecks[i].isSelected()) {
                            selectedIndexes.add(i);
                            sb.append(renamedFields[i].getText());
                            if (selectedIndexes.size() < countSelected(columnChecks)) {
                                sb.append(", ");
                            }
                        }
                    }
                    sb.append(") VALUES (");
                    for (int i = 0; i < selectedIndexes.size(); i++) {
                        int col = selectedIndexes.get(i);
                        String tipo = (String) typeSelectors[col].getSelectedItem();
                        Object valor = row.get(col);
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
                                texto = texto.replaceAll("[.,\\-*]", "");
                            }
                            if ("DATE".equals(tipo) && parseDateChecks[col].isSelected()) {
                                try {
                                    SimpleDateFormat inputFormat = new SimpleDateFormat("dd/MM/yyyy");
                                    inputFormat.setLenient(false);
                                    SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd");

                                    java.util.Date data = inputFormat.parse(texto);
                                    texto = outputFormat.format(data);
                                } catch (ParseException ex) {
                                    texto = "1970-01-01";
                                }
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
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(exportarButton, BorderLayout.SOUTH);
        dialog.add(mainPanel);
        dialog.setSize(900, 500);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
    private int countSelected(JCheckBox[] boxes) {
        int count = 0;
        for (JCheckBox box : boxes) {
            if (box != null && box.isSelected()) count++;
        }
        return count;
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

    private void atualizarDadosNoBancoEmMassa(int[] linhas, String coluna, String novoValor) {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DatabaseUtil.getPath());
             Statement stmt = conn.createStatement()) {
            for (int row : linhas) {
                Object id = table.getValueAt(row, 0);
                String sql = "UPDATE csv_data SET \"" + coluna + "\" = ? WHERE id = ?";
                System.out.println("SQL gerado: " + sql);
                System.out.println("Linha: " + row + ", ID: " + id + ", Novo valor: " + novoValor);

                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, novoValor);
                    ps.setObject(2, id);
                    int linhasAfetadas = ps.executeUpdate();
                    System.out.println("Linhas afetadas: " + linhasAfetadas);

                }
            }
            JOptionPane.showMessageDialog(this, "Dados atualizados com sucesso.");
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao atualizar dados: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        } finally {
            loadData();
        }
    }

}

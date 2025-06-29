package org.processadorcsv.viewmodel;

import org.processadorcsv.jdbd.db.DatabaseUtil;
import org.processadorcsv.viewmodel.util.CSVExporterWorker;
import org.processadorcsv.viewmodel.util.CarregadorDadosVazios;

import javax.swing.*;
import javax.swing.plaf.nimbus.State;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.sql.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.processadorcsv.viewmodel.util.ExportadorExcel;
import org.processadorcsv.viewmodel.util.TelaDuplicidades;

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
    JMenuItem menuItemApagarColuna = new JMenuItem("Apagar Coluna");
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
        JMenuItem menuItemSanitizacao = new JMenuItem("Sanitizações");
        JButton loadMissingButton = new JButton("Carregar dados vazios");
        JMenuItem menuItemDuplicidades = new JMenuItem("Carregar Duplicatas");


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
        topPanel.add(loadMissingButton);

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
        JMenuItem menuItemApagarColuna = new JMenuItem("Remover coluna");

        JMenuItem menuItemExportarCSV = new JMenuItem("Exportar para CSV");
        JMenuItem menuItemExportarSQL = new JMenuItem("Exportar para SQL insert");
        JMenuItem menuItemEdicaoMassa = new JMenuItem("Edição em Massa");
        JMenuItem menuItemExportarExcel = new JMenuItem("Exportar para Excel");
        JMenuItem menuItemPopularColuna = new JMenuItem("Popular Coluna");


        menuArquivo.add(menuItemRenomearColumas);
        menuArquivo.add(menuItemAdicionarColuna);
        menuArquivo.add(menuItemEdicaoMassa);
        menuArquivo.add(menuItemPopularColuna);

        menuExportacao.add(menuItemExportarCSV);
        menuExportacao.add(menuItemExportarExcel);
        menuExportacao.add(menuItemExportarSQL);
        menuArquivo.add(menuItemSanitizacao);
        menuArquivo.add(menuItemDuplicidades);
        menuArquivo.add(menuItemApagarColuna);

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

        menuItemExportarSQL.addActionListener(e -> {
            ExportarSQLDialog dialog = new ExportarSQLDialog(this);
            dialog.setVisible(true);
        });

        menuItemSanitizacao.addActionListener(e -> {
            TelaSanitizacao dialog = new TelaSanitizacao(this);
            dialog.setVisible(true);
        });

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

        menuItemExportarExcel.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Salvar como Excel");
            fileChooser.setSelectedFile(new File("dados_exportados.xls"));
            int userSelection = fileChooser.showSaveDialog(this);
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File fileToSave = fileChooser.getSelectedFile();

                new Thread(() -> {
                    try {
                        ExportadorExcel.exportar(columnNames, fileToSave, DatabaseUtil.getPath());
                        SwingUtilities.invokeLater(() ->
                                JOptionPane.showMessageDialog(this, "Exportação concluída!")
                        );
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        SwingUtilities.invokeLater(() ->
                                JOptionPane.showMessageDialog(this, "Erro ao exportar: " + ex.getMessage())
                        );
                    }
                }).start();
            }
        });

        menuItemApagarColuna.addActionListener(e -> {
            if (columnNames.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Nenhuma coluna carregada.");
                return;
            }
            String colunaSelecionada = (String) JOptionPane.showInputDialog(
                    this,
                    "Selecione a coluna a ser apagada:",
                    "Apagar Coluna",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    columnNames.toArray(),
                    columnNames.get(0)
            );
            if (colunaSelecionada != null) {
                int confirm = JOptionPane.showConfirmDialog(
                        this,
                        "Tem certeza que deseja apagar a coluna '" + colunaSelecionada + "'?",
                        "Confirmação",
                        JOptionPane.YES_NO_OPTION
                );
                if (confirm == JOptionPane.YES_OPTION) {
                    apagarColuna(colunaSelecionada);
                }
            }
        });

        loadMissingButton.addActionListener(e -> {
            CarregadorDadosVazios carregador = new CarregadorDadosVazios(this, tableModel, table, columnNames);
            carregador.exibirDialogoSelecaoColunas();
        });

        menuItemDuplicidades.addActionListener(e -> {
            TelaDuplicidades duplicidades = new TelaDuplicidades(columnNames);
            duplicidades.setVisible(true);
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
                /*for (int row : linhasSelecionadas) {
                    tableModel.setValueAt(novoValor, row, colunaIndex);
                }*/
                atualizarDadosNoBancoEmMassa(linhasSelecionadas, coluna, novoValor);
            }
        });

        menuItemPopularColuna.addActionListener(e -> {
            PopularColunaDialog dialog = new PopularColunaDialog(this, columnNames);
            dialog.setVisible(true);
            loadData();
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
        ExportarCSVDialog dialog = new ExportarCSVDialog(this, new ArrayList<>(columnNames));
        dialog.setVisible(true);
        if (!dialog.isConfirmado()) return;
        boolean incluirCabecalho = dialog.isIncluirCabecalho();
        File file = dialog.getArquivoSelecionado();
        List<String> colunasSelecionadas = dialog.getColunasSelecionadas();
        Set<String> colunasParaInt = dialog.getColunasParaInteiro();
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DatabaseUtil.getPath());
             Statement stmt = conn.createStatement();
             FileWriter writer = new FileWriter(file)) {
            if (incluirCabecalho) {
                writer.write(String.join(",", colunasSelecionadas));
                writer.write("\n");
            }
            String sql = "SELECT " + String.join(", ", colunasSelecionadas) + " FROM csv_data";
            ResultSet rs = stmt.executeQuery(sql);
            BlockingQueue<List<Object>> queue = new ArrayBlockingQueue<>(500);
            Thread worker = new Thread(new CSVExporterWorker(queue, colunasSelecionadas, writer));
            worker.start();
            while (rs.next()) {
                List<Object> row = new ArrayList<>();
                for (String coluna : colunasSelecionadas) {
                    Object valor = rs.getObject(coluna);
                    if (colunasParaInt.contains(coluna)) {
                        String strValor = String.valueOf(valor).trim();
                        try {
                            if (strValor.matches("\\d+\\.\\d+")) {
                                valor = Double.parseDouble(strValor);
                            } else {
                                strValor = strValor.replaceAll("[^\\d-]", "");
                                valor = Integer.parseInt(strValor);
                            }
                        } catch (NumberFormatException ex) {
                            valor = 0;
                        }
                    }
                    row.add(valor == null ? "" : valor);
                }
                queue.put(row);
            }
            queue.put(new ArrayList<>());
            worker.join();
            JOptionPane.showMessageDialog(this, "Exportado com sucesso.");
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao exportar CSV.");
        }
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
        if (coluna.equalsIgnoreCase("id")) {
            JOptionPane.showMessageDialog(this,
                    "A coluna 'id' não pode ser editada.",
                    "Operação inválida",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

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

    public String[] getColumnNames() {
        return columnNames.toArray(new String[0]);
    }

    public int getColumnIndex(String columnName) {
        return columnNames.indexOf(columnName);
    }

    public DefaultTableModel getTableModel() {
        return tableModel;
    }

    public void atualizarColunaNoBanco(String coluna, List<String> ids) {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DatabaseUtil.getPath())) {
            String sql = "UPDATE csv_data SET " + coluna + " = ? WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            int colIndex = getColumnIndex(coluna);
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                stmt.setString(1, (String) tableModel.getValueAt(i, colIndex));
                stmt.setString(2, ids.get(i)); // Usa o ID real da linha
                stmt.addBatch();
            }
            stmt.executeBatch();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao atualizar banco: " + e.getMessage());
        }
    }

    private void apagarColuna(String coluna) {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DatabaseUtil.getPath())) {

            String sql = "ALTER TABLE csv_data DROP COLUMN \"" + coluna + "\";";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.executeUpdate();

            columnNames.clear();
            loadColumnNames();
            loadData();
            JOptionPane.showMessageDialog(this, "Coluna '" + coluna + "' apagada com sucesso.");
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao apagar coluna: " + e.getMessage());
        }
    }

}

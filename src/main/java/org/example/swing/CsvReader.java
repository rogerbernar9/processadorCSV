package org.example.swing;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class CsvReader extends JFrame {
    private JTable table;
    private DefaultTableModel tableModel;
    private JDialog loadingDialog;
    private File currentFile;
    private JRadioButton headerYes;
    private JRadioButton headerNo;
    private JTextField searchField;
    private TableRowSorter<DefaultTableModel> rowSorter;
    private JPanel inputPanel;
    private List<JTextField> inputFields;
    private JButton btnAdd;



    public CsvReader() {
        setTitle("Leitor de CSV - Java Swing");
        setSize(950, 620);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        tableModel = new DefaultTableModel();
        table = new JTable(tableModel);

        rowSorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(rowSorter);

        JScrollPane scrollPane = new JScrollPane(table);

        JButton btnLoad = new JButton("Carregar CSV");
        btnLoad.addActionListener(e -> loadCsv());

        searchField = new JTextField(20);

        JButton btnSave = new JButton("Salvar CSV");
        btnSave.addActionListener(e -> saveCsv());

        JButton btnFiltrar = new JButton("Filtrar");

        JButton btnLimpar = new JButton("Limpar");

        JButton btnExportSql = new JButton("Exportar como SQL");

        JButton btnAddColumn = new JButton("Adicionar Coluna");

        JButton btnEditCell = new JButton("Editar Célula");


        JPanel panel = new JPanel();
        panel.add(btnLoad);
        panel.add(btnSave);
        panel.add(new JLabel("Pesquisar: "));
        panel.add(searchField);
        panel.add(btnFiltrar);
        panel.add(btnLimpar);
        panel.add(btnExportSql);
        panel.add(btnAddColumn);
        panel.add(btnEditCell);

        btnFiltrar.addActionListener(e -> filterTable());

        btnLimpar.addActionListener(e -> limparTable());

        btnExportSql.addActionListener(e -> exportAsSql());

        btnEditCell.addActionListener(e -> editSelectedCell());


        // Painel para opções de cabeçalho
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new FlowLayout());

        JLabel headerLabel = new JLabel("Primeira linha é cabeçalho?");
        headerYes = new JRadioButton("Sim");
        headerNo = new JRadioButton("Não");
        ButtonGroup headerGroup = new ButtonGroup();
        headerGroup.add(headerYes);
        headerGroup.add(headerNo);
        headerNo.setSelected(true);

        headerPanel.add(headerLabel);
        headerPanel.add(headerYes);
        headerPanel.add(headerNo);

        // Layout da tela
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BorderLayout());
        topPanel.add(panel, BorderLayout.NORTH);
        topPanel.add(headerPanel, BorderLayout.CENTER);

        add(scrollPane, BorderLayout.CENTER);
        inputPanel = new JPanel();
        btnAdd = new JButton("Adicionar Registro");
        btnAdd.addActionListener(e -> addRecord());
        btnAddColumn.addActionListener(e -> addNewColumnAndSave());

        inputPanel.add(btnAdd);
        add(topPanel, BorderLayout.NORTH);
        add(inputPanel, BorderLayout.SOUTH);

    }

    private void filterTable() {
        String searchText = searchField.getText();
        if (searchText.trim().length() == 0) {
            rowSorter.setRowFilter(null);
        } else {
            rowSorter.setRowFilter(RowFilter.regexFilter("(?i)" + searchText));
        }
    }

    private void limparTable() {
        searchField.setText("");
        rowSorter.setRowFilter(null);
    }

    private void loadCsv() {
        JFileChooser fileChooser = new JFileChooser();
        int option = fileChooser.showOpenDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            currentFile = fileChooser.getSelectedFile();
            showLoadingDialog();
            new CsvLoader(currentFile).execute();
        }
    }

    private void saveCsv() {
        if (currentFile == null) {
            JOptionPane.showMessageDialog(this, "Nenhum arquivo carregado para salvar!", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(currentFile))) {
            boolean hasHeader = headerYes.isSelected();
            if(hasHeader) {
                // Escreve o cabeçalho
                for (int i = 1; i < tableModel.getColumnCount(); i++) {
                    bw.write(tableModel.getColumnName(i));
                    if (i < tableModel.getColumnCount() - 1) {
                        bw.write(",");
                    }
                }
                bw.newLine();
            }

            // Escreve os dados
            for (int row = 0; row < tableModel.getRowCount(); row++) {
                for (int col = 1; col < tableModel.getColumnCount(); col++) {
                    bw.write(tableModel.getValueAt(row, col).toString());
                    if (col < tableModel.getColumnCount() - 1) {
                        bw.write(",");
                    }
                }
                bw.newLine();
            }
            JOptionPane.showMessageDialog(this, "Arquivo salvo com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Erro ao salvar o arquivo!", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportAsSql() {
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Nenhum dado para exportar!", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String tableName = JOptionPane.showInputDialog(this, "Nome da tabela:", "Exportar como SQL", JOptionPane.PLAIN_MESSAGE);
        if (tableName == null || tableName.trim().isEmpty()) {
            return;
        }
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File(tableName + ".sql"));
        int option = fileChooser.showSaveDialog(this);
        if (option != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File sqlFile = fileChooser.getSelectedFile();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(sqlFile))) {
            // Monta os nomes das colunas (exceto o ID, que está na posição 0)
            int colCount = tableModel.getColumnCount();
            StringBuilder columns = new StringBuilder();
            for (int i = 1; i < colCount; i++) {
                columns.append("`").append(tableModel.getColumnName(i)).append("`");
                if (i < colCount - 1) columns.append(", ");
            }
            for (int row = 0; row < tableModel.getRowCount(); row++) {
                StringBuilder values = new StringBuilder();
                for (int col = 1; col < colCount; col++) {
                    Object val = tableModel.getValueAt(row, col);
                    String escaped = val != null ? val.toString().replace("'", "''") : "";
                    values.append("'").append(escaped).append("'");
                    if (col < colCount - 1) values.append(", ");
                }
                String insert = String.format("INSERT INTO `%s` (%s) VALUES (%s);", tableName, columns, values);
                writer.write(insert);
                writer.newLine();
            }
            JOptionPane.showMessageDialog(this, "Arquivo SQL exportado com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Erro ao salvar arquivo SQL!", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void editSelectedCell() {
        int selectedRow = table.getSelectedRow();
        int selectedColumn = table.getSelectedColumn();
        if (selectedRow == -1 || selectedColumn == -1) {
            JOptionPane.showMessageDialog(this, "Selecione uma célula para editar.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        // Converte para o índice do modelo (por causa do RowSorter)
        selectedRow = table.convertRowIndexToModel(selectedRow);
        selectedColumn = table.convertColumnIndexToModel(selectedColumn);
        Object currentValue = tableModel.getValueAt(selectedRow, selectedColumn);
        JTextField textField = new JTextField(currentValue != null ? currentValue.toString() : "");
        int result = JOptionPane.showConfirmDialog(
                this,
                textField,
                "Editar valor da célula",
                JOptionPane.OK_CANCEL_OPTION
        );
        if (result == JOptionPane.OK_OPTION) {
            String newValue = textField.getText();
            tableModel.setValueAt(newValue, selectedRow, selectedColumn);
            saveCsv();
        }
    }

    private void showLoadingDialog() {
        loadingDialog = new JDialog(this, "Carregando...", true);
        loadingDialog.setSize(200, 100);
        loadingDialog.setLocationRelativeTo(this);
        loadingDialog.setLayout(new FlowLayout());

        JLabel loadingLabel = new JLabel("Processando...");
        loadingDialog.add(loadingLabel);

        loadingDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        SwingUtilities.invokeLater(() -> loadingDialog.setVisible(true));
    }

    private void hideLoadingDialog() {
        if (loadingDialog != null) {
            loadingDialog.dispose();
        }
    }

    private class CsvLoader extends SwingWorker<Void, Void> {
        private File file;

        public CsvLoader(File file) {
            this.file = file;
        }

        @Override
        protected Void doInBackground() {
            readCsvFileParallel(file);
            return null;
        }

        @Override
        protected void done() {
            hideLoadingDialog();
        }
    }

    private void readCsvFileParallel(File file) {
        tableModel.setRowCount(0);
        boolean hasHeader = headerYes.isSelected();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            String[] header = br.readLine().split(",");
            if (hasHeader) {
                String[] newHeader = new String[header.length + 1];
                newHeader[0] = "ID";
                System.arraycopy(header, 0, newHeader, 1, header.length);
                SwingUtilities.invokeLater(() -> {
                    tableModel.setColumnIdentifiers(newHeader);
                    generateInputFields(header.length);
                    table.removeColumn(table.getColumnModel().getColumn(0));
                });
            } else {
                // Fecha o BufferedReader e reabre para ler o arquivo desde o começo
                readCsvFileNoHeader(file, header.length);
                return;
            }
            List<String[]> allRows = new ArrayList<>();
            while ((line = br.readLine()) != null) {
                allRows.add(line.split(","));
            }
            processCsvChunks(allRows);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao ler o arquivo CSV!", "Erro", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace(); // útil para depuração
        }
    }
    private void readCsvFileNoHeader(File file, int columnCount) {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            List<String[]> allRows = new ArrayList<>();
            String line;
            while ((line = br.readLine()) != null) {
                allRows.add(line.split(","));
            }
            // Gerar cabeçalhos genéricos
            String[] newHeader = new String[columnCount + 1];
            newHeader[0] = "ID";
            for (int i = 1; i < newHeader.length; i++) {
                newHeader[i] = "Column" + i;
            }
            SwingUtilities.invokeLater(() -> tableModel.setColumnIdentifiers(newHeader));
            processCsvChunks(allRows);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao ler o arquivo CSV (sem cabeçalho)!", "Erro", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    private void processCsvChunks(List<String[]> allRows) throws InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<Future<List<Object[]>>> futures = new ArrayList<>();
        int chunkSize = 1000;
        for (int i = 0; i < allRows.size(); i += chunkSize) {
            final int start = i;
            final int end = Math.min(i + chunkSize, allRows.size());
            futures.add(executor.submit(() -> {
                List<Object[]> rows = new ArrayList<>();
                for (int j = start; j < end; j++) {
                    String[] lineData = allRows.get(j);
                    Object[] row = new Object[lineData.length + 1];
                    row[0] = j + 1;
                    System.arraycopy(lineData, 0, row, 1, lineData.length);
                    rows.add(row);
                }
                return rows;
            }));
        }
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.MINUTES);
        for (Future<List<Object[]>> future : futures) {
            List<Object[]> chunk = future.get();
            SwingUtilities.invokeLater(() -> {
                for (Object[] row : chunk) {
                    tableModel.addRow(row);
                }
            });
        }
    }

    private void generateInputFields(int columnCount) {
        inputPanel.removeAll();
        inputFields = new ArrayList<>();
        for (int i = 0; i < columnCount; i++) {
            JTextField field = new JTextField(10);
            inputFields.add(field);
            inputPanel.add(field);
        }
        inputPanel.add(btnAdd);
        inputPanel.revalidate();
        inputPanel.repaint();
    }
    private void addRecord() {
        if (inputFields == null || inputFields.isEmpty()) return;
        String[] rowData = new String[inputFields.size() + 1];
        rowData[0] = String.valueOf(tableModel.getRowCount() + 1); // ID automático
        for (int i = 0; i < inputFields.size(); i++) {
            rowData[i + 1] = inputFields.get(i).getText();
        }
        tableModel.addRow(rowData); // Adiciona na tabela
        // Também adiciona ao arquivo CSV
        if (currentFile != null && currentFile.exists()) {
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(currentFile, true))) {
                // Monta a linha CSV (sem o ID, pois está no modelo mas não no arquivo original)
                for (int i = 1; i < rowData.length; i++) {
                    bw.write(rowData[i]);
                    if (i < rowData.length - 1) {
                        bw.write(",");
                    }
                }
                bw.newLine();
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Erro ao escrever no arquivo CSV!", "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
        // Limpa os campos após inserção
        for (JTextField field : inputFields) {
            field.setText("");
        }
    }

    private void addNewColumnAndSave() {
        if (currentFile == null) {
            JOptionPane.showMessageDialog(this, "Nenhum CSV carregado.");
            return;
        }
        int rowCount = tableModel.getRowCount();
        int colCount = tableModel.getColumnCount();
        // Salva os dados atuais
        Object[][] data = new Object[rowCount][colCount];
        for (int i = 0; i < rowCount; i++) {
            for (int j = 0; j < colCount; j++) {
                data[i][j] = tableModel.getValueAt(i, j);
            }
        }
        // Cria nova estrutura de colunas (com uma coluna sem nome)
        String[] newColumns = new String[colCount + 1];
        for (int i = 0; i < colCount; i++) {
            newColumns[i] = tableModel.getColumnName(i);
        }
        newColumns[colCount] = ""; // coluna sem nome
        // Atualiza o modelo
        tableModel.setDataVector(new Object[0][0], newColumns);
        for (int i = 0; i < rowCount; i++) {
            Object[] newRow = new Object[newColumns.length];
            System.arraycopy(data[i], 0, newRow, 0, data[i].length);
            newRow[newRow.length - 1] = ""; // célula vazia
            tableModel.addRow(newRow);
        }
        // Salva automaticamente no CSV
        saveCsv();
    }



}
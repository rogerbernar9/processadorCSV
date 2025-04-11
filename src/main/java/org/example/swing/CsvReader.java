package org.example.swing;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


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
    private JLabel progressLabel;
    private AtomicInteger linesProcessed = new AtomicInteger(0);

    private int currentPage = 1;
    private int rowsPerPage = 100; // Quantas linhas por página
    private List<Object[]> fullData = new ArrayList<>(); // Armazena todos os dados para paginação
    private JPanel paginationPanel;
    private JButton btnAnterior;
    private JButton btnProxima;
    private JLabel lblPagina;
    private static final List<String[]> POISON_PILL = Collections.emptyList();

    private List<Object[]> dadosLidos = Collections.synchronizedList(new ArrayList<>());

    private String[] headers;

    private String insertSql = "";

    private Connection conexaoCompartilhada;
    private ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private BlockingQueue<List<String>> csvQueue = new LinkedBlockingQueue<>();
    private AtomicInteger totalRows = new AtomicInteger(0);
    private AtomicInteger insertedRows = new AtomicInteger(0);


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

//        btnFiltrar.addActionListener(e -> filterTable());

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

//        paginacao
        paginationPanel = new JPanel();
        btnAnterior = new JButton("Anterior");
        btnProxima = new JButton("Próxima");
        lblPagina = new JLabel("Página: 1");
        btnProxima.addActionListener(e -> {
            currentPage++;
            updateTablePage();

        });
        btnAnterior.addActionListener(e -> {
            if (currentPage > 1) {
                currentPage--;
                updateTablePage();
            }
        });
        paginationPanel.add(btnAnterior);
        paginationPanel.add(lblPagina);
        paginationPanel.add(btnProxima);
        add(paginationPanel, BorderLayout.AFTER_LAST_LINE);

    }


    private void updateTablePage() {
        displayDataFromSqlite(currentPage);
        lblPagina.setText("Página: " + currentPage);
    }

    private void updateTableData() {
        int start = (currentPage - 1) * rowsPerPage;
        int end = Math.min(start + rowsPerPage, fullData.size());
        SwingUtilities.invokeLater(() -> {
            tableModel.setRowCount(0);
            for (int i = start; i < end; i++) {
                tableModel.addRow(fullData.get(i));
            }
            lblPagina.setText("Página: " + currentPage);
        });
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
            createSqliteDatabase(currentFile);
            loadDataIntoSqlite(currentFile);
            executor.execute(() -> {
                try {
                    // Aguardar o processamento terminar (csvQueue ficar vazio e sinal de fim)
                    while (csvQueue.peek() != null) {
                        Thread.sleep(100);
                    }
                    SwingUtilities.invokeLater(() -> displayDataFromSqlite(1));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    e.printStackTrace();
                }
            });
        }
    }
    private void createSqliteDatabase(File csvFile) {
        try {
            Class.forName("org.sqlite.JDBC");
            conexaoCompartilhada = DriverManager.getConnection("jdbc:sqlite:data.db");
            Statement stmt = conexaoCompartilhada.createStatement();
            stmt.executeUpdate("DROP TABLE IF EXISTS csv_data");
            String createTableSql = "CREATE TABLE csv_data (id INTEGER PRIMARY KEY AUTOINCREMENT, ";
            try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
                String headerLine = br.readLine();
                String[] header = headerLine.split(",");
                for (int i = 0; i < header.length; i++) {
                    createTableSql += "`" + header[i] + "` TEXT";
                    if (i < header.length - 1) {
                        createTableSql += ", ";
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            createTableSql += ")";
            stmt.executeUpdate(createTableSql);
            stmt.close();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao criar o banco de dados SQLite.", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadDataIntoSqlite(File csvFile) {
        boolean hasHeader = headerYes.isSelected();
        executor.execute(() -> { // Thread para processar as inserções
            try {
                processCsvLines();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Erro ao processar dados CSV.", "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String headerLine = br.readLine();
            String[] header = headerLine.split(",");
            insertSql = "INSERT INTO csv_data (";
            for (int i = 0; i < header.length; i++) {
                insertSql += "`" + header[i] + "`";
                if (i < header.length - 1) {
                    insertSql += ", ";
                }
            }
            insertSql += ") VALUES (";
            for (int i = 0; i < header.length; i++) {
                insertSql += "?";
                if (i < header.length - 1) {
                    insertSql += ", ";
                }
            }
            insertSql += ")";
            String line;
            List<String> batch = new ArrayList<>();
            int batchSize = 500;
            while ((line = br.readLine()) != null) {
                batch.add(line); // Adiciona a linha inteira
                totalRows.incrementAndGet();
                if (batch.size() >= batchSize) {
                    csvQueue.put(new ArrayList<>(batch));
                    batch.clear();
                }
            }
            if (!batch.isEmpty()) {
                csvQueue.put(batch);
            }
            csvQueue.put(Collections.emptyList()); // Sinal de fim de processamento
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao ler o arquivo CSV.", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void processCsvLines() throws InterruptedException {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:data.db");
             PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
            conn.setAutoCommit(false);
            while (true) {
                List<String> batch = csvQueue.take();
                if (batch.isEmpty()) {
                    break;
                }
                for (String line : batch) {
                    String[] values = parseCsvLine(line);
                    for (int i = 0; i < values.length; i++) {
                        pstmt.setString(i + 1, values[i]);
                    }
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
                conn.commit();
                insertedRows.addAndGet(batch.size());
                System.out.println("Inseridos: " + insertedRows + " / Total: " + totalRows);
            }
            conn.setAutoCommit(true);
        } catch (SQLException e) {
            e.printStackTrace();
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Erro ao inserir dados no SQLite.", "Erro", JOptionPane.ERROR_MESSAGE));
        }
    }

    private String[] parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder currentVal = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == ',' && !inQuotes) {
                values.add(currentVal.toString());
                currentVal.setLength(0);
            } else if (c == '"') {
                inQuotes = !inQuotes;
            } else {
                currentVal.append(c);
            }
        }
        values.add(currentVal.toString());
        return values.toArray(new String[0]);
    }
    private void displayDataFromSqlite(int page) {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:data.db");
             Statement stmt = conn.createStatement()) {
            int offset = (page - 1) * rowsPerPage;
            String query = "SELECT * FROM csv_data LIMIT " + rowsPerPage + " OFFSET " + offset;
            ResultSet rs = stmt.executeQuery(query);
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            // Configura o cabeçalho da tabela
            String[] columnNames = new String[columnCount];
            for (int i = 1; i <= columnCount; i++) {
                columnNames[i - 1] = metaData.getColumnName(i);
            }
            tableModel.setColumnIdentifiers(columnNames);
            // Limpa a tabela e adiciona os dados
            tableModel.setRowCount(0);
            while (rs.next()) {
                Object[] rowData = new Object[columnCount];
                for (int i = 1; i <= columnCount; i++) {
                    rowData[i - 1] = rs.getString(i);
                }
                tableModel.addRow(rowData);
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao buscar dados do SQLite.", "Erro", JOptionPane.ERROR_MESSAGE);
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
        progressLabel = new JLabel("Linhas processadas: 0");
        loadingDialog.add(progressLabel);

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
//            readCsvFileParallel(file);
            return null;
        }

        @Override
        protected void done() {
//            hideLoadingDialog();
        }
    }

    private void readCsvFileParallel(File file) {
        tableModel.setRowCount(0);

        boolean hasHeader = headerYes.isSelected();
        BlockingQueue<List<String[]>> queue = new ArrayBlockingQueue<>(10);
        int numWorkers = Runtime.getRuntime().availableProcessors();
        ExecutorService workerPool = Executors.newFixedThreadPool(numWorkers);

        for (int i = 0; i < numWorkers; i++) {
            workerPool.submit(() -> {
                try {
                    while (true) {
                        List<String[]> batch = queue.take();
                        if (batch == POISON_PILL) break;

                        List<Object[]> rows = new ArrayList<>();
                        for (String[] lineData : batch) {
                            Object[] row = new Object[lineData.length + 1];
                            row[0] = "";
                            System.arraycopy(lineData, 0, row, 1, lineData.length);
                            rows.add(row);
                            dadosLidos.add(row); // Armazena os dados lidos

                        }
                        int processed = linesProcessed.addAndGet(batch.size());
                        System.out.println("Lidos: " + batch.size());
                        System.out.println("Total: " + processed);

                        SwingUtilities.invokeLater(() -> {
                            for (Object[] row : rows) {
                                tableModel.addRow(row);
                            }
//                            progressLabel.setText("Linhas processadas: " + processed);
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        // Produtor
        new Thread(() -> {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String headerLine = br.readLine();
                String[] header = headerLine.split(",");

                String[] newHeader = new String[header.length + 1];
                newHeader[0] = "ID";

                if (hasHeader) {
                    System.arraycopy(header, 0, newHeader, 1, header.length);
                    headers = header; // guarda o cabeçalho original
                } else {
                    for (int i = 1; i < newHeader.length; i++) {
                        newHeader[i] = "Coluna " + i;
                    }
                    headers = newHeader; // guarda os nomes gerados
                }

                SwingUtilities.invokeLater(() -> {
                    tableModel.setColumnIdentifiers(headers); // se necessário
                    fullData.clear(); // limpa dados anteriores
                    fullData.addAll(dadosLidos); // onde dadosLidos é sua lista final de Object[]
                    currentPage = 1;
                    updateTableData();
                });

                // Se não tiver cabeçalho, considera a primeira linha como dados
                if (!hasHeader) {
                    List<String[]> firstBatch = new ArrayList<>();
                    firstBatch.add(header.clone());
                    queue.put(firstBatch);
                }


                String line;
                List<String[]> batch = new ArrayList<>();
                int batchSize = 1000;
                while ((line = br.readLine()) != null) {
                    batch.add(line.split(","));
                    if (batch.size() >= batchSize) {
                        queue.put(new ArrayList<>(batch));
                        batch.clear();
                    }
                }
                if (!batch.isEmpty()) {
                    queue.put(batch);
                }

                for (int i = 0; i < numWorkers; i++) {
                    queue.put(POISON_PILL);
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Erro ao ler CSV!", "Erro", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }).start();
    }

    private void readCsvFileNoHeader(File file, int columnCount) {
        String[] header = new String[0];

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            List<String[]> allRows = new ArrayList<>();
            String line;

            while ((line = br.readLine()) != null) {
                header = line.split(",");
                allRows.add(line.split(","));
            }

            String[] newHeader = new String[columnCount + 1];
            newHeader[0] = "ID";
            for (int i = 1; i < newHeader.length; i++) {
                newHeader[i] = "Column" + i;
            }
            SwingUtilities.invokeLater(() -> tableModel.setColumnIdentifiers(newHeader));
            generateInputFields(header.length);
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
        rowData[0] = String.valueOf(tableModel.getRowCount() + 1);
        for (int i = 0; i < inputFields.size(); i++) {
            rowData[i + 1] = inputFields.get(i).getText();
        }
        //... pega as informações inseridas na tela.
        executor.execute(() -> {
            try {
                PreparedStatement threadStmt = conexaoCompartilhada.prepareStatement(insertSql);
                //... usar o insertSql aqui para inserir os dados.
                threadStmt.executeUpdate();
                threadStmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Erro ao inserir dados no SQLite.", "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    public void dispose() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException ex) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        try {
            if (conexaoCompartilhada != null && !conexaoCompartilhada.isClosed()){
                conexaoCompartilhada.close();
            }
        }catch(SQLException e){
            e.printStackTrace();
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

        String[] newColumns = new String[colCount + 1];
        for (int i = 0; i < colCount; i++) {
            newColumns[i] = tableModel.getColumnName(i);
        }
        newColumns[colCount] = "";

        tableModel.setDataVector(new Object[0][0], newColumns);
        for (int i = 0; i < rowCount; i++) {
            Object[] newRow = new Object[newColumns.length];
            System.arraycopy(data[i], 0, newRow, 0, data[i].length);
            newRow[newRow.length - 1] = "";
            tableModel.addRow(newRow);
        }

        saveCsv();
    }


    public List<Object[]> getDadosLidos() {
        return dadosLidos;
    }



}
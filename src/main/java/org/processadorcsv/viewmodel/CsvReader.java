package org.processadorcsv.viewmodel;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.sql.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class CsvReader extends JFrame {
    private final DefaultTableModel tableModel = new DefaultTableModel();
    private final JTable table = new JTable(tableModel);
    private final JCheckBox headerYes = new JCheckBox("Possui cabeçalho", true);
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    BlockingQueue<List<String>> csvQueue = new ArrayBlockingQueue<>(50);
    private final AtomicInteger totalRows = new AtomicInteger(0);
    private final AtomicInteger insertedRows = new AtomicInteger(0);
    private final int batchSize = 500;
    private final AtomicBoolean leituraFinalizada = new AtomicBoolean(false);
    private File currentFile;
    private String[] headers;
    private String insertSql;
    private final int rowsPerPage = 1000;

    public CsvReader() {
        setTitle("CSV Processador");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(800, 600);
        setLayout(new BorderLayout());
        JButton loadButton = new JButton("Carregar CSV");
        loadButton.addActionListener(e -> loadCsv());
        add(loadButton, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(headerYes, BorderLayout.SOUTH);

        JButton viewDataButton = new JButton("Visualizar dados");
        viewDataButton.addActionListener(e -> {
            JFrame dataFrame = new JFrame("Dados CSV");
            dataFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            dataFrame.setSize(800, 600);
            dataFrame.setLocationRelativeTo(this);
            dataFrame.add(new VisualizadorDados());
            dataFrame.setVisible(true);
        });
        add(viewDataButton, BorderLayout.EAST);
    }

    private void loadCsv() {
        JFileChooser fileChooser = new JFileChooser();
        int option = fileChooser.showOpenDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            currentFile = fileChooser.getSelectedFile();
            createSqliteDatabase(currentFile);
            loadDataIntoSqlite(currentFile);
        }
    }

    private void createSqliteDatabase(File csvFile) {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:data.db")) {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("DROP TABLE IF EXISTS csv_data");

            String createTableSql = "CREATE TABLE csv_data (id INTEGER PRIMARY KEY AUTOINCREMENT, ";

            try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
                String firstLine = br.readLine();
                if (firstLine != null) {
                    String[] columns = firstLine.split(",");
                    headers = new String[columns.length];

                    for (int i = 0; i < columns.length; i++) {
                        String clean = columns[i].replaceAll("[^a-zA-Z0-9_]", "");
                        if (clean.matches("^\\d+$")) {
                            clean = "col_" + clean;
                        }
                        headers[i] = clean;
                        createTableSql += "`" + clean + "` TEXT";
                        if (i < columns.length - 1) {
                            createTableSql += ", ";
                        }
                    }
                }
            }

            createTableSql += ")";
            stmt.executeUpdate(createTableSql);

            insertSql = "INSERT INTO csv_data (" +
                    String.join(", ", Arrays.stream(headers).map(h -> "`" + h + "`").toList()) + ") VALUES (" +
                    String.join(", ", Collections.nCopies(headers.length, "?")) + ")";
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao criar banco de dados.", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadDataIntoSqlite(File csvFile) {
        boolean hasHeader = headerYes.isSelected();
        leituraFinalizada.set(false);

        executor.execute(() -> {
            try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
                String line = br.readLine();
                if (!hasHeader && line != null) {
                    csvQueue.put(List.of(line));
                    totalRows.incrementAndGet();
                }

                List<String> chunk = new ArrayList<>(batchSize);
                while ((line = br.readLine()) != null) {
                    chunk.add(line);
                    totalRows.incrementAndGet();

                    if (chunk.size() >= batchSize) {
                        csvQueue.put(new ArrayList<>(chunk));
                        chunk.clear();
                    }
                }

                if (!chunk.isEmpty()) {
                    csvQueue.put(chunk);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                leituraFinalizada.set(true);
            }
        });
        int consumidores = 2;

        for (int j = 0; j < consumidores; j++) {
            executor.execute(() -> {

                try (Connection conn = DriverManager.getConnection("jdbc:sqlite:data.db");
                     PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                    conn.setAutoCommit(false);
                    while (!leituraFinalizada.get() || !csvQueue.isEmpty()) {
                        List<String> chunk = csvQueue.poll(1, TimeUnit.SECONDS);
                        if (chunk == null || chunk.isEmpty()) continue;

                        for (String line : chunk) {
                            String[] values = parseCsvLine(line);
                            for (int i = 0; i < values.length; i++) {
                                pstmt.setString(i + 1, values[i]);
                            }
                            pstmt.addBatch();
                        }

                        pstmt.executeBatch();
                        conn.commit();
                        insertedRows.addAndGet(chunk.size());

                        System.out.println("Inseridos: " + insertedRows.get());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private String[] parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder currentVal = new StringBuilder();
        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                values.add(currentVal.toString());
                currentVal.setLength(0);
            } else {
                currentVal.append(c);
            }
        }
        values.add(currentVal.toString());
        return values.toArray(new String[0]);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CsvReader().setVisible(true));
    }
}

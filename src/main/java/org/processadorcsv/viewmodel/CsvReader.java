package org.processadorcsv.viewmodel;

import org.processadorcsv.jdbd.db.DatabaseUtil;

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
import java.util.regex.Pattern;

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
    private final JLabel statusLabel = new JLabel("Linhas inseridas: 0");
    private String separator = ",";
    private final JPanel contentPanel = new JPanel(new BorderLayout());

    private final String versao = "";


    public CsvReader() {
        setTitle("CSV Processador");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(800, 600);
        setLayout(new BorderLayout());
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(headerYes, BorderLayout.SOUTH);

        JPanel welcomePanel = new JPanel(new GridBagLayout());
        JLabel titleLabel = new JLabel("CSV Processador");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 28));
        welcomePanel.add(titleLabel);

        contentPanel.add(welcomePanel, BorderLayout.CENTER);
        add(contentPanel, BorderLayout.CENTER);

        JMenuBar menuBar = new JMenuBar();
        // cria um menu
        JMenu menuArquivo = new JMenu("Opções");
        JMenuItem menuItemCarregarCSV = new JMenuItem("Carregar CSV");
        JMenuItem menuItemVisualizarDados = new JMenuItem("Visualizar Dados");
        menuArquivo.add(menuItemCarregarCSV);
        menuArquivo.add(menuItemVisualizarDados);
        // Adiciona o menu à barra de menu
        menuBar.add(menuArquivo);

        // Define a barra de menu na janela
        setJMenuBar(menuBar);

        // Adiciona itens ao menu
        menuArquivo.add(menuItemCarregarCSV);
        menuArquivo.add(menuItemVisualizarDados);

        // Adiciona o menu à barra de menu
        menuBar.add(menuArquivo);

        menuItemCarregarCSV.addActionListener(e -> loadCsv());
        menuItemVisualizarDados.addActionListener(e -> {
            VisualizadorDados visualizadorDados = new VisualizadorDados();
            visualizadorDados.setVisible(true);
            this.setVisible(false);
        });

        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.add(statusLabel);
        contentPanel.add(statusPanel, BorderLayout.SOUTH);

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

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:"+ DatabaseUtil.getPath())) {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("DROP TABLE IF EXISTS csv_data");

            String createTableSql = "CREATE TABLE csv_data (id INTEGER PRIMARY KEY AUTOINCREMENT, ";

            try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
                String firstLine = br.readLine();
                String separator = detectaSeparator(firstLine); // detecta o separador
                this.separator = separator;

                if (firstLine != null) {
                    String[] columns = firstLine.split(Pattern.quote(separator));
                    headers = new String[columns.length];

                    Random random = new Random();
                    for (int i = 0; i < columns.length; i++) {
                        String randomSuffix = String.format("%03d", random.nextInt(1000)); // 3 dígitos aleatórios
                        String clean = "col_" + i + "_" + randomSuffix;
                        headers[i] = clean;
                        createTableSql += "`" + clean + "` TEXT";
                        if (i < columns.length - 1) {
                            createTableSql += ", ";
                        }
                    }
                }
            }

            createTableSql += ")";
            System.out.println(createTableSql);
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

                try (Connection conn = DriverManager.getConnection("jdbc:sqlite:"+ DatabaseUtil.getPath());
                     PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                    conn.setAutoCommit(false);
                    while (!leituraFinalizada.get() || !csvQueue.isEmpty()) {
                        List<String> chunk = csvQueue.poll(1, TimeUnit.SECONDS);
                        if (chunk == null || chunk.isEmpty()) continue;

                        for (String line : chunk) {
                            String[] values = parseCsvLine(line);
                            for (int i = 0; i < headers.length; i++) {
                                String value = i < values.length ? values[i] : null;
                                pstmt.setString(i + 1, value);
                            }
                            pstmt.addBatch();
                        }

                        pstmt.executeBatch();
                        conn.commit();
                        insertedRows.addAndGet(chunk.size());

                        // adiciona linhas inseridas na tela inicial
                        SwingUtilities.invokeLater(() -> {
                            statusLabel.setText("Linhas inseridas: " + insertedRows.get());
                        });
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
            } else if (String.valueOf(c).equals(separator) && !inQuotes) {
                values.add(currentVal.toString());
                currentVal.setLength(0);
            } else {
                currentVal.append(c);
            }
        }
        values.add(currentVal.toString().replaceAll("^\"|\"$", ""));
        return values.toArray(new String[0]);
    }

    private String detectaSeparator(String line) {
        String[] possibleSeparators = {",", ";", "\t", "|"};
        int maxCount = 0;
        String detected = ",";
        for (String sep : possibleSeparators) {
            int count = line.split(Pattern.quote(sep), -1).length;
            if (count > maxCount) {
                maxCount = count;
                detected = sep;
            }
        }
        return detected;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CsvReader().setVisible(true));
    }
}

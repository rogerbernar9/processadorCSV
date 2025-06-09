package org.processadorcsv.viewmodel;

import org.mozilla.universalchardet.UniversalDetector;
import org.processadorcsv.ferramentaBD.view.LoginView;
import org.processadorcsv.jdbd.db.DatabaseUtil;
import org.processadorcsv.viewmodel.util.PerformanceTuner;
import org.processadorcsv.viewmodel.util.Preferencias;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.nio.charset.Charset;
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
    private int batchSize = 500;
    private final AtomicBoolean leituraFinalizada = new AtomicBoolean(false);
    private File currentFile;
    private String[] headers;
    private String insertSql;
    private final int rowsPerPage = 1000;
    private final JLabel statusLabel = new JLabel("Linhas inseridas: 0");
    private String separator = ",";
    private final JPanel contentPanel = new JPanel(new BorderLayout());
    private JMenuItem menuItemCarregarCSV;
    private JMenuItem menuItemVisualizarDados;
    private JMenuItem menuItemConectarBancoDados;
    private JMenuItem menuItemApagarSQLite;
    private JMenuItem menuItemPreferencias;



    public CsvReader() {
        setTitle("CSV Processador");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(800, 600);
        setLayout(new BorderLayout());
        add(new JScrollPane(table), BorderLayout.CENTER);
//        add(headerYes, BorderLayout.SOUTH);

        JPanel welcomePanel = new JPanel(new GridBagLayout());
        JLabel titleLabel = new JLabel("CSV Processador");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 28));
        welcomePanel.add(titleLabel);

        contentPanel.add(welcomePanel, BorderLayout.CENTER);
        add(contentPanel, BorderLayout.CENTER);

        JMenuBar menuBar = new JMenuBar();
        // cria um menu
        JMenu menuArquivo = new JMenu("Opções");
        JMenu menuPreferencias = new JMenu("Preferências");
        JMenu menuBD = new JMenu("Banco de Dados");
        menuItemCarregarCSV = new JMenuItem("Carregar CSV");
        menuItemVisualizarDados = new JMenuItem("Visualizar Dados");

        menuItemConectarBancoDados = new JMenuItem("Conectar em um Banco de dados Externo");
        menuItemApagarSQLite = new JMenuItem("Descartar banco local");
        menuItemPreferencias = new JMenuItem("Otimização");
        menuArquivo.add(menuItemCarregarCSV);
        menuArquivo.add(menuItemVisualizarDados);


        // Define a barra de menu na janela
        setJMenuBar(menuBar);

        // Adiciona itens ao menu
        menuArquivo.add(menuItemCarregarCSV);
        menuArquivo.add(menuItemVisualizarDados);
        menuArquivo.add(menuItemApagarSQLite);
        menuBD.add(menuItemConectarBancoDados);
        menuPreferencias.add(menuItemPreferencias);
        // Adiciona o menu à barra de menu
        menuBar.add(menuArquivo);
        menuBar.add(menuBD);
        menuBar.add(menuPreferencias);

        menuItemCarregarCSV.addActionListener(e -> loadCsv());

        menuItemApagarSQLite.setEnabled(checkDatabaseExists());
        menuItemVisualizarDados.setEnabled(checkDatabaseExists());

        menuItemVisualizarDados.addActionListener(e -> {
            VisualizadorDados visualizadorDados = new VisualizadorDados();
            visualizadorDados.setVisible(true);
            this.setVisible(false);
        });

        menuItemConectarBancoDados.addActionListener(e -> {
            LoginView loginView = new LoginView();
            loginView.setVisible(true);
        });

        menuItemApagarSQLite.addActionListener(e -> {
            deleteSqliteDatabase();
        });

        menuItemPreferencias.addActionListener(e -> {
            PreferenciasView preferenciasView = new PreferenciasView(this);
            preferenciasView.setVisible(true);
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
        menuItemCarregarCSV.setEnabled(false);
        menuItemVisualizarDados.setEnabled(false);
        menuItemApagarSQLite.setEnabled(false);

        boolean hasHeader = headerYes.isSelected();
        leituraFinalizada.set(false);

        String chartSetName = this.detectCharset(csvFile);
        System.out.println(chartSetName);

        int optimalBatchSize = PerformanceTuner.calculaBatchSizeOtimizado();
        int optimalThreads = PerformanceTuner.calculaThreadsOtimizada();
        this.batchSize = optimalBatchSize; // atualiza o batchSize

        System.out.println("Usando batchSize: " + optimalBatchSize + ", Threads: " + optimalThreads);

        executor.execute(() -> {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(
                    new FileInputStream(csvFile), Charset.forName(chartSetName)))) {

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

                if (Preferencias.isOtimizarCarregamento()) {

                    executor.execute(() -> {
                        while (insertedRows.get() < totalRows.get()) {
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException ignored) {
                            }
                        }

                        System.out.println(insertedRows.get());
                        if (insertedRows.get() >= 100_000) {
                            SwingUtilities.invokeLater(() -> {
                                CarregadorIndices.show("Otimizando banco de dados...");
                            });

                            executor.execute(() -> {
                                verificarOuCriarIndices();

                                SwingUtilities.invokeLater(() -> {
                                    CarregadorIndices.hide();
                                    menuItemCarregarCSV.setEnabled(true);
                                    menuItemVisualizarDados.setEnabled(true);
                                    menuItemApagarSQLite.setEnabled(true);
                                    JOptionPane.showMessageDialog(null, "Índices criados com sucesso para otimizar a busca.");
                                });
                            });
                        }
                    });
                }


                SwingUtilities.invokeLater(() -> {
                    menuItemCarregarCSV.setEnabled(true);
                    menuItemVisualizarDados.setEnabled(true);
                    menuItemApagarSQLite.setEnabled(true);
                });
            }
        });

        for (int j = 0; j < optimalThreads; j++) {
            executor.execute(() -> {
                try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DatabaseUtil.getPath());
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

                        SwingUtilities.invokeLater(() ->
                                statusLabel.setText("Linhas inseridas: " + insertedRows.get())
                        );
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

    private String detectCharset(File file) {
        byte[] buf = new byte[4096];
        try (FileInputStream fis = new FileInputStream(file)) {
            UniversalDetector detector = new UniversalDetector(null);
            int nread;
            while ((nread = fis.read(buf)) > 0 && !detector.isDone()) {
                detector.handleData(buf, 0, nread);
            }
            detector.dataEnd();
            String encoding = detector.getDetectedCharset();
            return encoding != null ? encoding : "UTF-8";
        } catch (IOException e) {
            e.printStackTrace();
            return "UTF-8";
        }
    }

    private void deleteSqliteDatabase() {

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:"+ DatabaseUtil.getPath())) {
            Statement stmt = conn.createStatement();
            int descarte = stmt.executeUpdate("DROP TABLE IF EXISTS csv_data");
            System.out.println(descarte);
            JOptionPane.showMessageDialog(null, "Banco de dados descartado com sucesso");


        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao apagar banco de dados.", "Erro", JOptionPane.ERROR_MESSAGE);
        } finally {
            reloadWindow();
        }
    }

    private boolean checkDatabaseExists() {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DatabaseUtil.getPath())) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getTables(null, null, "csv_data", null)) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void reloadWindow() {
        this.dispose();
        SwingUtilities.invokeLater(() ->
        {
            CsvReader csvReader = new CsvReader();
            csvReader.setVisible(true);
        });
    }

    private void verificarOuCriarIndices() {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DatabaseUtil.getPath())) {
            Statement stmt = conn.createStatement();

            List<String> colunas = new ArrayList<>();
            ResultSet rs = stmt.executeQuery("PRAGMA table_info(csv_data)");
            while (rs.next()) {
                String nomeColuna = rs.getString("name");
                if (!nomeColuna.equalsIgnoreCase("id")) {
                    colunas.add(nomeColuna);
                }
            }
            rs.close();

            Set<String> indicesExistentes = new HashSet<>();
            rs = stmt.executeQuery("PRAGMA index_list(csv_data)");
            while (rs.next()) {
                String nomeIndice = rs.getString("name");
                indicesExistentes.add(nomeIndice);
            }
            rs.close();

            for (String indice : indicesExistentes) {
                if (indice.startsWith("idx_")) {
                    stmt.execute("DROP INDEX IF EXISTS " + indice);
                }
            }

            for (String coluna : colunas) {
                String nomeIndice = "idx_" + coluna;
                System.out.println("Criando índice para coluna: "+coluna);
                String sql = "CREATE INDEX IF NOT EXISTS " + nomeIndice + " ON csv_data(\"" + coluna + "\")";
                stmt.execute(sql);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }




    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CsvReader().setVisible(true));
    }
}

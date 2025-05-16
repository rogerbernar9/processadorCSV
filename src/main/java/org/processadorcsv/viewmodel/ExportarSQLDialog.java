package org.processadorcsv.viewmodel;

import org.processadorcsv.jdbd.db.DatabaseUtil;
import org.processadorcsv.viewmodel.util.SQLGeneratorWorker;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.FileWriter;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class ExportarSQLDialog extends JDialog {

    private JTextField tableNameField;
    private JPanel customPanel;
    private final int MAX_COLUMNS = 100;
    private JCheckBox[] columnChecks = new JCheckBox[MAX_COLUMNS];
    private JTextField[] renamedFields = new JTextField[MAX_COLUMNS];
    private JComboBox<String>[] typeSelectors = new JComboBox[MAX_COLUMNS];
    private JCheckBox[] sanitizeChecks = new JCheckBox[MAX_COLUMNS];
    private JCheckBox[] parseDateChecks = new JCheckBox[MAX_COLUMNS];
    private List<String> columnNames = new ArrayList<>();
    private List<List<Object>> dadosTabela = new ArrayList<>();
    private final String[] tiposDados = {"VARCHAR", "INTEGER", "DATE", "BOOLEAN", "DOUBLE"};
    public ExportarSQLDialog(JFrame parent) {
        super(parent, "Exportar SQL com personalização", true);
        setLayout(new BorderLayout());
        JPanel mainPanel = new JPanel(new BorderLayout());
        JPanel tableNamePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        tableNamePanel.add(new JLabel("Nome da Tabela:"));
        tableNameField = new JTextField("csv_data", 20);
        tableNamePanel.add(tableNameField);
        customPanel = new JPanel(new GridLayout(0, 6, 5, 5));
        JScrollPane scrollPane = new JScrollPane(customPanel);
        JButton carregarColunasButton = new JButton("Carregar Colunas");
        carregarColunasButton.addActionListener(this::carregarColunas);
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(tableNamePanel, BorderLayout.WEST);
        topPanel.add(carregarColunasButton, BorderLayout.EAST);
        JButton exportarButton = new JButton("Exportar SQL");
        exportarButton.addActionListener(e -> exportarSQL());
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(exportarButton, BorderLayout.SOUTH);
        add(mainPanel);
        setSize(900, 500);
        setLocationRelativeTo(parent);
    }
    private void carregarColunas(ActionEvent ev) {
        columnNames.clear();
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DatabaseUtil.getPath());
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableNameField.getText().trim() + " LIMIT 1")) {
            ResultSetMetaData meta = rs.getMetaData();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                columnNames.add(meta.getColumnName(i));
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao carregar colunas do banco.");
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
    }
    private void exportarSQL() {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DatabaseUtil.getPath());
             Statement stmt = conn.createStatement()) {
            String tableName = tableNameField.getText().trim();
            if (tableName.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Informe um nome válido para a tabela.");
                return;
            }
            ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableName);
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Salvar como SQL");
            int userSelection = fileChooser.showSaveDialog(this);
            if (userSelection != JFileChooser.APPROVE_OPTION) return;
            try (FileWriter writer = new FileWriter(fileChooser.getSelectedFile())) {
                List<Integer> selectedIndexes = new ArrayList<>();
                for (int i = 0; i < columnNames.size(); i++) {
                    if (columnChecks[i] != null && columnChecks[i].isSelected()) {
                        selectedIndexes.add(i);
                    }
                }
                while (rs.next()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("INSERT INTO ").append(tableName).append(" (");
                    for (int i = 0; i < selectedIndexes.size(); i++) {
                        sb.append(renamedFields[selectedIndexes.get(i)].getText());
                        if (i < selectedIndexes.size() - 1) sb.append(", ");
                    }
                    sb.append(") VALUES (");
                    for (int i = 0; i < selectedIndexes.size(); i++) {
                        int col = selectedIndexes.get(i);
                        Object valor = rs.getObject(col + 1);
                        String tipo = (String) typeSelectors[col].getSelectedItem();
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
                    writer.write(sb.toString());
                }
            }
            JOptionPane.showMessageDialog(this, "Arquivo SQL exportado com sucesso!");
            dispose();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao exportar SQL.");
        }
    }

    private int countSelected(JCheckBox[] checkboxes) {
        int count = 0;
        for (JCheckBox checkbox : checkboxes) {
            if (checkbox != null && checkbox.isSelected()) {
                count++;
            }
        }
        return count;
    }
}
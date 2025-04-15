package org.processadorcsv.viewmodel;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;


public class VisualizadorDados extends JFrame {

    private final DefaultTableModel tableModel = new DefaultTableModel();
    private final JTable table = new JTable(tableModel);
    private final JTextField filterField = new JTextField(50);
    private final JButton previousButton = new JButton("Anterior");
    private final JButton nextButton = new JButton("Próximo");
    private final JButton telaAnterior = new JButton("Tela Principal");
    private int currentPage = 0;
    private final int pageSize = 100;


    public VisualizadorDados() {
        setTitle("CSV Processador");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 600);
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel();
        JButton loadButton = new JButton("Carregar dados");
        topPanel.add(new JLabel("Filtro:"));
        topPanel.add(filterField);
        topPanel.add(loadButton);
        topPanel.add(telaAnterior);
        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        JPanel bottomPanel = new JPanel();
        bottomPanel.add(previousButton);
        bottomPanel.add(nextButton);
        add(bottomPanel, BorderLayout.SOUTH);

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
    }

    private void loadData() {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:data.db")) {
            Statement stmt = conn.createStatement();
            ResultSet rsColumns = stmt.executeQuery("PRAGMA table_info(csv_data)");
            Vector<String> columns = new Vector<>();
            while (rsColumns.next()) {
                String name = rsColumns.getString("name");
                if (!name.equalsIgnoreCase("id")) {
                    columns.add(name);
                }
            }
            rsColumns.close();
            String filterText = filterField.getText().trim();
            String whereClause = "";
            if (!filterText.isEmpty()) {
                StringBuilder filterBuilder = new StringBuilder("WHERE ");
                for (int i = 0; i < columns.size(); i++) {
                    filterBuilder.append(columns.get(i)).append(" LIKE '%").append(filterText).append("%'");
                    if (i < columns.size() - 1) filterBuilder.append(" OR ");
                }
                whereClause = filterBuilder.toString();
            }
            String sql = String.format("SELECT %s FROM csv_data %s LIMIT %d OFFSET %d",
                    String.join(", ", columns),
                    whereClause,
                    pageSize,
                    currentPage * pageSize);
            ResultSet rs = stmt.executeQuery(sql);
            tableModel.setRowCount(0);
            tableModel.setColumnIdentifiers(columns);
            while (rs.next()) {
                Vector<String> row = new Vector<>();
                for (String col : columns) {
                    row.add(rs.getString(col));
                }
                tableModel.addRow(row);
            }
            rs.close();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao carregar dados.", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
}

package org.processadorcsv.viewmodel;

import org.processadorcsv.jdbd.db.DatabaseUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;


public class VisualizadorDados extends JFrame {

    private final DefaultTableModel tableModel = new DefaultTableModel();
    private final JTable table = new JTable(tableModel);
    private final JTextField filterField1 = new JTextField(25);
    private final JTextField filterField2 = new JTextField(25);
    private final JTextField filterField3 = new JTextField(25);
    private final JButton previousButton = new JButton("Anterior");
    private final JButton nextButton = new JButton("Próximo");
    private final JButton telaAnterior = new JButton("Tela Principal");
    private int currentPage = 0;
    private final int pageSize = 100;
    private final Vector<String> columnNames = new Vector<>();


    public VisualizadorDados() {
        setTitle("CSV Processador");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1300, 600);
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

}

package org.processadorcsv.ferramentaBD.model.service.dialog;

import org.processadorcsv.ferramentaBD.model.service.IDBService;
import org.processadorcsv.jdbd.db.DatabaseUtil;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.*;
import java.util.List;

public class ImportarDoInternoDialog extends JDialog {
    private JTable table;
    private DefaultTableModel tableModel;
    private List<String> columnNames = new ArrayList<>();
    private int currentPage = 0;
    private int pageSize = 50;
    private JLabel lblStatus;
    private IDBService dbService;
    private String tabelaDestino;
    public ImportarDoInternoDialog(Frame parent, IDBService dbService, String tabelaDestino) {
        super(parent, "Importar do Interno", true);
        this.dbService = dbService;
        this.tabelaDestino = tabelaDestino;
        initUI();
        loadData();
        setVisible(true);
    }
    private void initUI() {
        setLayout(new BorderLayout());
        tableModel = new DefaultTableModel();
        table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);
        JPanel bottomPanel = new JPanel(new BorderLayout());
        lblStatus = new JLabel("Carregando...");
        bottomPanel.add(lblStatus, BorderLayout.WEST);
        JButton btnImportar = new JButton("Importar Dados");
        btnImportar.addActionListener(e -> importarDados());
        bottomPanel.add(btnImportar, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);
        setSize(800, 600);
        setLocationRelativeTo(getParent());
    }
    private void loadColumnNames() {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DatabaseUtil.getPath())) {
            Statement stmt = conn.createStatement();
            ResultSet rsColumns = stmt.executeQuery("PRAGMA table_info(csv_data)");
            columnNames.clear();
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
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DatabaseUtil.getPath())) {
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
            ResultSet rs = stmt.executeQuery(sql);
            tableModel.setRowCount(0);
            tableModel.setColumnIdentifiers(new Vector<>(columnNames));
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
    private void importarDados() {
        try (
                Connection origemConn = DriverManager.getConnection("jdbc:sqlite:" + DatabaseUtil.getPath());
                Statement stmt = origemConn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM csv_data")
        ) {
            List<Map<String, Object>> dados = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> linha = new HashMap<>();
                for (String coluna : columnNames) {
                    linha.put(coluna, rs.getObject(coluna));
                }
                dados.add(linha);
            }
            int count = 0;
            for (Map<String, Object> linha : dados) {

                Map<String, Object> dadosConvertidos = new HashMap<>(linha);
                System.out.println(linha);
                System.out.println(tabelaDestino);
                dbService.inserirRegistroSeCamposCorrespondentes(tabelaDestino, dadosConvertidos);
                count++;
            }
            JOptionPane.showMessageDialog(this, count + " registros importados com sucesso!");
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao importar dados.", "Erro", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String buildWhereClause() {
        return "";
    }
    private void carregaQuantidadeDados() {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DatabaseUtil.getPath())) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS total FROM csv_data");
            if (rs.next()) {
                int total = rs.getInt("total");
                lblStatus.setText("Total de registros: " + total);
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
            lblStatus.setText("Erro ao contar registros.");
        }
    }
}

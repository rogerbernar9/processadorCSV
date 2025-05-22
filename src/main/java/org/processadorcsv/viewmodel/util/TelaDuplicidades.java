package org.processadorcsv.viewmodel.util;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.util.Vector;
import org.processadorcsv.jdbd.db.DatabaseUtil;

public class TelaDuplicidades extends JFrame {
    private final JComboBox<String> colunaComboBox = new JComboBox<>();
    private final DefaultTableModel tableModel = new DefaultTableModel();
    private final JTable tabela = new JTable(tableModel);
    private final JButton buscarButton = new JButton("Buscar Duplicidades");
    private final JButton exportarButton = new JButton("Exportar XLS");

    public TelaDuplicidades(Vector<String> colunas) {
        setTitle("Buscar Duplicidades");
        setSize(800, 600);
        setLayout(new BorderLayout());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        JPanel topPanel = new JPanel(new FlowLayout());

        topPanel.add(new JLabel("Selecionar coluna:"));
        for (String coluna : colunas) {
            colunaComboBox.addItem(coluna);
        }
        topPanel.add(colunaComboBox);
        topPanel.add(buscarButton);
        topPanel.add(exportarButton);

        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(tabela), BorderLayout.CENTER);
        buscarButton.addActionListener(e -> buscarDuplicidades());
        exportarButton.addActionListener(e -> exportarParaXLS());

    }
    private void buscarDuplicidades() {
        String colunaSelecionada = (String) colunaComboBox.getSelectedItem();
        if (colunaSelecionada == null) return;
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:"+DatabaseUtil.getPath())) {
            Statement stmt = conn.createStatement();

            String sqlDuplicados = "SELECT " + colunaSelecionada + " FROM csv_data GROUP BY " + colunaSelecionada + " HAVING COUNT(*) > 1";
            ResultSet rsDuplicados = stmt.executeQuery(sqlDuplicados);
            Vector<String> valoresDuplicados = new Vector<>();
            while (rsDuplicados.next()) {
                valoresDuplicados.add(rsDuplicados.getString(1));
            }
            if (valoresDuplicados.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Nenhuma duplicidade encontrada.");
                tableModel.setRowCount(0);
                tableModel.setColumnCount(0);
                return;
            }

            String placeholders = String.join(",", java.util.Collections.nCopies(valoresDuplicados.size(), "?"));
            String sqlFinal = "SELECT * FROM csv_data WHERE " + colunaSelecionada + " IN (" + placeholders + ")";
            PreparedStatement ps = conn.prepareStatement(sqlFinal);
            for (int i = 0; i < valoresDuplicados.size(); i++) {
                ps.setString(i + 1, valoresDuplicados.get(i));
            }
            ResultSet rs = ps.executeQuery();
            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();
            // Define os nomes das colunas
            Vector<String> nomesColunas = new Vector<>();
            for (int i = 1; i <= columnCount; i++) {
                nomesColunas.add(meta.getColumnName(i));
            }
            tableModel.setColumnIdentifiers(nomesColunas);
            // popular
            Vector<Vector<Object>> data = new Vector<>();
            while (rs.next()) {
                Vector<Object> linha = new Vector<>();
                for (int i = 1; i <= columnCount; i++) {
                    linha.add(rs.getObject(i));
                }
                data.add(linha);
            }
            tableModel.setRowCount(0);
            for (Vector<Object> row : data) {
                tableModel.addRow(row);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao buscar duplicidades: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void exportarParaXLS() {
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Nenhum dado para exportar.");
            return;
        }
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Salvar como");
        fileChooser.setSelectedFile(new java.io.File("duplicidades.xls"));
        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection != JFileChooser.APPROVE_OPTION) {
            return;
        }
        java.io.File fileToSave = fileChooser.getSelectedFile();
        try (org.apache.poi.hssf.usermodel.HSSFWorkbook workbook = new org.apache.poi.hssf.usermodel.HSSFWorkbook()) {
            org.apache.poi.hssf.usermodel.HSSFSheet sheet = workbook.createSheet("Duplicidades");

            org.apache.poi.hssf.usermodel.HSSFRow headerRow = sheet.createRow(0);
            for (int i = 0; i < tableModel.getColumnCount(); i++) {
                headerRow.createCell(i).setCellValue(tableModel.getColumnName(i));
            }

            for (int i = 0; i < tableModel.getRowCount(); i++) {
                org.apache.poi.hssf.usermodel.HSSFRow row = sheet.createRow(i + 1);
                for (int j = 0; j < tableModel.getColumnCount(); j++) {
                    Object value = tableModel.getValueAt(i, j);
                    row.createCell(j).setCellValue(value != null ? value.toString() : "");
                }
            }
            try (java.io.FileOutputStream out = new java.io.FileOutputStream(fileToSave)) {
                workbook.write(out);
            }
            JOptionPane.showMessageDialog(this, "Arquivo exportado com sucesso para:\n" + fileToSave.getAbsolutePath());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao exportar: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
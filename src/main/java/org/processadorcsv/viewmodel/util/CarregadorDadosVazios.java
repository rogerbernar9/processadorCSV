package org.processadorcsv.viewmodel.util;

import org.processadorcsv.jdbd.db.DatabaseUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;
import java.util.List;

public class CarregadorDadosVazios {
    private final JFrame parentFrame;
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final List<String> columnNames;
    public CarregadorDadosVazios(JFrame parentFrame, DefaultTableModel tableModel, JTable table, List<String> columnNames) {
        this.parentFrame = parentFrame;
        this.tableModel = tableModel;
        this.table = table;
        this.columnNames = columnNames;
    }
    public void exibirDialogoSelecaoColunas() {
        JPanel panel = new JPanel(new GridLayout(0, 1));
        Map<String, JCheckBox> checkboxes = new LinkedHashMap<>();
        for (String column : columnNames) {
            JCheckBox checkbox = new JCheckBox(column);
            checkboxes.put(column, checkbox);
            panel.add(checkbox);
        }
        int result = JOptionPane.showConfirmDialog(parentFrame, panel, "Selecione as colunas com dados vazios", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            List<String> colunasSelecionadas = new ArrayList<>();
            for (Map.Entry<String, JCheckBox> entry : checkboxes.entrySet()) {
                if (entry.getValue().isSelected()) {
                    colunasSelecionadas.add(entry.getKey());
                }
            }
            if (!colunasSelecionadas.isEmpty()) {
                carregarDadosComVaziosDestacados(colunasSelecionadas);
            }
        }
    }
    private void carregarDadosComVaziosDestacados(List<String> colunasSelecionadas) {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DatabaseUtil.getPath())) {
            Statement stmt = conn.createStatement();
            String selectColumns = String.join(", ", columnNames);
            StringBuilder whereClause = new StringBuilder("WHERE ");
            for (int i = 0; i < colunasSelecionadas.size(); i++) {
                String coluna = colunasSelecionadas.get(i);
                whereClause.append("(").append(coluna).append(" IS NULL OR TRIM(").append(coluna).append(") = '')");
                if (i < colunasSelecionadas.size() - 1) {
                    whereClause.append(" OR ");
                }
            }
            String sql = String.format("SELECT %s FROM csv_data %s", selectColumns, whereClause);
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
            destacarCelulasVazias(colunasSelecionadas);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(parentFrame, "Erro ao carregar dados vazios.", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
    private void destacarCelulasVazias(List<String> colunasVazias) {
        Set<Integer> indices = new HashSet<>();
        for (String coluna : colunasVazias) {
            int index = columnNames.indexOf(coluna);
            if (index != -1) {
                indices.add(index);
            }
        }
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (indices.contains(column) && (value == null || value.toString().trim().isEmpty())) {
                    c.setBackground(Color.YELLOW);
                } else {
                    c.setBackground(isSelected ? table.getSelectionBackground() : Color.WHITE);
                }
                return c;
            }
        });
    }
}
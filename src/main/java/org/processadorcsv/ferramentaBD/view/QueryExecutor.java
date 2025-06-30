package org.processadorcsv.ferramentaBD.view;

import org.processadorcsv.ferramentaBD.model.service.IDBService;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;

public class QueryExecutor extends JDialog {
    private final IDBService dbService;
    private JTextArea areaSQL = new JTextArea(5, 40);
    private JTable tabelaResultados = new JTable();
    public QueryExecutor(JFrame parent, IDBService dbService) {
        super(parent, "Executar SQL", true);
        this.dbService = dbService;
        setLayout(new BorderLayout());
        JPanel painelTopo = new JPanel(new BorderLayout());
        painelTopo.add(new JScrollPane(areaSQL), BorderLayout.CENTER);
        JButton btnExecutar = new JButton("Executar");
        JButton btnVerificar = new JButton("Verificar Sintaxe");

        painelTopo.add(btnExecutar, BorderLayout.EAST);
        painelTopo.add(btnVerificar, BorderLayout.WEST);

        add(painelTopo, BorderLayout.NORTH);
        add(new JScrollPane(tabelaResultados), BorderLayout.CENTER);
        btnExecutar.addActionListener(e -> executarSQL());
        btnVerificar.addActionListener(e -> verificarSintaxe());

        setSize(700, 400);
        setLocationRelativeTo(parent);
        setVisible(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    private void executarSQL() {
        String sql = areaSQL.getText().trim();
        if (sql.isEmpty()) return;
        try (Statement stmt = dbService.getConnection().createStatement()) {
            if (sql.toLowerCase().startsWith("select")) {
                ResultSet rs = stmt.executeQuery(sql);
                ResultSetMetaData meta = rs.getMetaData();
                DefaultTableModel modelo = new DefaultTableModel();
                for (int i = 1; i <= meta.getColumnCount(); i++) {
                    modelo.addColumn(meta.getColumnName(i));
                }
                while (rs.next()) {
                    Object[] linha = new Object[meta.getColumnCount()];
                    for (int i = 1; i <= meta.getColumnCount(); i++) {
                        linha[i - 1] = rs.getObject(i);
                    }
                    modelo.addRow(linha);
                }
                tabelaResultados.setModel(modelo);
            } else {
                int linhasAfetadas = stmt.executeUpdate(sql);
                JOptionPane.showMessageDialog(this, linhasAfetadas + " linha(s) afetada(s).");
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao executar SQL: " + ex.getMessage());
        }
    }

    private void verificarSintaxe() {
        String sql = areaSQL.getText().trim();
        System.out.println(sql);
        if (sql.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Digite uma query para verificar.");
            return;
        }
        try {
            dbService.getConnection().prepareStatement(sql).close();
            JOptionPane.showMessageDialog(this, "Sintaxe SQL válida!");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro de sintaxe SQL: " + ex.getMessage());
        }
    }

}

package org.processadorcsv.viewmodel;

import org.processadorcsv.jdbd.db.DatabaseUtil;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Vector;

public class PopularColunaDialog extends JDialog {
    private JComboBox<String> comboColunas;
    private JTextField campoValor;
    private JProgressBar progressBar;
    private JButton botaoSalvar;
    private JLabel statusLabel;
    private final Vector<String> colunas;
    public PopularColunaDialog(Frame parent, Vector<String> colunas) {
        super(parent, "Popular Coluna", true);
        this.colunas = colunas;
        setSize(400, 200);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());
        JPanel painelEntrada = new JPanel(new GridLayout(3, 2, 10, 10));
        painelEntrada.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        painelEntrada.add(new JLabel("Coluna:"));
        comboColunas = new JComboBox<>(colunas);
        painelEntrada.add(comboColunas);
        painelEntrada.add(new JLabel("Valor a inserir:"));
        campoValor = new JTextField();
        painelEntrada.add(campoValor);
        add(painelEntrada, BorderLayout.CENTER);
        JPanel painelInferior = new JPanel(new BorderLayout());
        progressBar = new JProgressBar(0, 100);
        progressBar.setVisible(false);
        statusLabel = new JLabel(" ");
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        botaoSalvar = new JButton("Salvar");
        botaoSalvar.addActionListener(e -> iniciarOperacao());
        painelInferior.add(botaoSalvar, BorderLayout.NORTH);
        painelInferior.add(progressBar, BorderLayout.CENTER);
        painelInferior.add(statusLabel, BorderLayout.SOUTH);
        add(painelInferior, BorderLayout.SOUTH);
    }
    private void iniciarOperacao() {
        String colunaSelecionada = (String) comboColunas.getSelectedItem();
        String valor = campoValor.getText();
        if (colunaSelecionada == null || valor.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Informe um valor válido e selecione a coluna.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }
        botaoSalvar.setEnabled(false);
        progressBar.setVisible(true);
        statusLabel.setText("Processando...");
        new Thread(() -> {
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:"+ DatabaseUtil.getPath())) {
                int total = contarRegistros(conn);
                int atual = 0;
                conn.setAutoCommit(false);
                String sql = "UPDATE csv_data SET \"" + colunaSelecionada + "\" = ?";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, valor);
                int atualizados = stmt.executeUpdate();
                conn.commit();
                for (int i = 0; i <= 100; i += 20) {
                    final int progresso = i;
                    SwingUtilities.invokeLater(() -> progressBar.setValue(progresso));
                    Thread.sleep(100);
                }
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, atualizados + " registros atualizados.");
                    statusLabel.setText("Concluído");
                    dispose();
                });
            } catch (InterruptedException | SQLException ex) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "Erro ao atualizar: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
                    statusLabel.setText("Erro");
                    botaoSalvar.setEnabled(true);
                });
            }
        }).start();
    }
    private int contarRegistros(Connection conn) throws SQLException {
        var rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM csv_data");
        return rs.next() ? rs.getInt(1) : 0;
    }
}

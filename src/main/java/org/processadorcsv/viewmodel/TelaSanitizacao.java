package org.processadorcsv.viewmodel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.processadorcsv.jdbd.db.DatabaseUtil;
import org.processadorcsv.viewmodel.VisualizadorDados;
import org.processadorcsv.viewmodel.util.SanitizacaoUtil;

public class TelaSanitizacao extends JDialog {
    private JComboBox<String> comboColunas;
    private JButton botaoSanitizar;
    private final VisualizadorDados pai;
    private JProgressBar progressBar;
    private JLabel statusLabel;
    private JList<String> listaColunas;
    private JRadioButton radioEspacos;
    private JRadioButton radioCaixaAlta;
    private JRadioButton radioPreencherCpf;
    private JRadioButton radioRemoverEspeciais;
    private ButtonGroup grupoOpcoes;

    public TelaSanitizacao(VisualizadorDados pai) {
        super(pai, "Sanitizar Campos", true);
        this.pai = pai;
        setLayout(new BorderLayout());
        setSize(400, 310);
        JPanel painelCentro = new JPanel();
        painelCentro.add(new JLabel("Escolha a coluna a sanitizar:"));
//        comboColunas = new JComboBox<>(pai.getColumnNames());
//        painelCentro.add(comboColunas);
        listaColunas = new JList<>(pai.getColumnNames());
        listaColunas.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        painelCentro.add(new JScrollPane(listaColunas), BorderLayout.CENTER);

        botaoSanitizar = new JButton("Sanitizar");
        JPanel painelOpcoes = new JPanel(new GridLayout(3, 1));
        painelOpcoes.setBorder(BorderFactory.createTitledBorder("Tipo de Sanitização"));
        radioEspacos = new JRadioButton("Remover espaços em branco");
        radioCaixaAlta = new JRadioButton("Converter para CAIXA ALTA");
        radioPreencherCpf = new JRadioButton("Preencher CPF com zeros à esquerda");
        radioRemoverEspeciais = new JRadioButton("Remover caracteres especiais");
        grupoOpcoes = new ButtonGroup();
        grupoOpcoes.add(radioEspacos);
        grupoOpcoes.add(radioCaixaAlta);
        grupoOpcoes.add(radioPreencherCpf);
        grupoOpcoes.add(radioRemoverEspeciais);
        radioEspacos.setSelected(true); // padrão
        painelOpcoes.add(radioEspacos);
        painelOpcoes.add(radioCaixaAlta);
        painelOpcoes.add(radioPreencherCpf);
        painelOpcoes.add(radioRemoverEspeciais);
        painelCentro.setLayout(new BorderLayout());
        painelCentro.add(new JScrollPane(listaColunas), BorderLayout.CENTER);
        painelCentro.add(painelOpcoes, BorderLayout.SOUTH);

        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);
        statusLabel = new JLabel(" ");
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        JPanel painelInferior = new JPanel(new BorderLayout());
        painelInferior.add(progressBar, BorderLayout.CENTER);
        painelInferior.add(statusLabel, BorderLayout.SOUTH);
        add(painelInferior, BorderLayout.NORTH);

        add(painelCentro, BorderLayout.CENTER);
        add(botaoSanitizar, BorderLayout.SOUTH);

        botaoSanitizar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                aplicarSanitizacao();
            }
        });
    }
    private void aplicarSanitizacao() {
        List<String> colunasSelecionadas = listaColunas.getSelectedValuesList();
        if (colunasSelecionadas.isEmpty()) return;
        progressBar.setMaximum(colunasSelecionadas.size());
        progressBar.setValue(0);
        progressBar.setVisible(true);
        statusLabel.setText("Sanitizando...");
        ExecutorService executor = Executors.newFixedThreadPool(colunasSelecionadas.size());
        for (String coluna : colunasSelecionadas) {
            executor.submit(() -> {
                try {
                    sanitizarColunaNoBanco(coluna);
                    SwingUtilities.invokeLater(() -> progressBar.setValue(progressBar.getValue() + 1));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
        }
        new Thread(() -> {
            executor.shutdown();
            try {
                executor.awaitTermination(10, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this, "Sanitização concluída com sucesso.");
                statusLabel.setText("Finalizado.");
                dispose();
            });
        }).start();
    }

    private void sanitizarColunaNoBanco(String coluna) {
        String sqlSelect = "SELECT id, " + coluna + " FROM csv_data";
        String sqlUpdate = "UPDATE csv_data SET " + coluna + " = ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DatabaseUtil.getPath());
             PreparedStatement stmtSelect = conn.prepareStatement(sqlSelect);
             PreparedStatement stmtUpdate = conn.prepareStatement(sqlUpdate)) {
            var rs = stmtSelect.executeQuery();
            while (rs.next()) {
                String id = rs.getString("id");
                String valorOriginal = rs.getString(coluna);
                String sanitizado;
                if (radioEspacos.isSelected()) {
                    sanitizado = SanitizacaoUtil.removerEspacos(valorOriginal);
                } else if (radioRemoverEspeciais.isSelected()) {
                    sanitizado = SanitizacaoUtil.removerCaracteresEspeciais(valorOriginal);
                } else if (radioCaixaAlta.isSelected()) {
                    sanitizado = SanitizacaoUtil.paraCaixaAlta(valorOriginal);
                } else if (radioPreencherCpf.isSelected()) {
                    sanitizado = SanitizacaoUtil.preencherCpfComZeros(valorOriginal);
                } else {
                    sanitizado = valorOriginal;
                }
                if (!sanitizado.equals(valorOriginal)) {
                    stmtUpdate.setString(1, sanitizado);
                    stmtUpdate.setString(2, id);
                    stmtUpdate.addBatch();
                }
            }
            stmtUpdate.executeBatch();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao sanitizar coluna " + coluna + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void atualizarDadosSanitizadosNoBanco(String coluna, Map<String, String> valoresPorId) {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DatabaseUtil.getPath())) {
            String sql = "UPDATE csv_data SET " + coluna + " = ? WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (Map.Entry<String, String> entry : valoresPorId.entrySet()) {
                    stmt.setString(1, entry.getValue());
                    stmt.setString(2, entry.getKey());
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao atualizar banco: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
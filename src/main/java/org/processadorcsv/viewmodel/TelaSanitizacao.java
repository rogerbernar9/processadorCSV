package org.processadorcsv.viewmodel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.processadorcsv.viewmodel.VisualizadorDados;
import org.processadorcsv.viewmodel.util.SanitizacaoUtil;

public class TelaSanitizacao extends JDialog {
    private JComboBox<String> comboColunas;
    private JButton botaoSanitizar;
    private final VisualizadorDados pai;
    private JProgressBar progressBar;
    private JLabel statusLabel;
    private JList<String> listaColunas;


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
        int idIndex = pai.getColumnIndex("id");
        int totalTarefas = colunasSelecionadas.size();
        progressBar.setMaximum(totalTarefas);
        progressBar.setValue(0);
        progressBar.setVisible(true);
        statusLabel.setText("Sanitizando...");
        ExecutorService executor = Executors.newFixedThreadPool(totalTarefas);
        for (String coluna : colunasSelecionadas) {
            executor.submit(() -> {
                try {
                    int colIndex = pai.getColumnIndex(coluna);
                    List<String> ids = new ArrayList<>();
                    for (int i = 0; i < pai.getTableModel().getRowCount(); i++) {
                        String valorOriginal = (String) pai.getTableModel().getValueAt(i, colIndex);
                        String sanitizado = SanitizacaoUtil.removerCaracteresEspeciais(valorOriginal);
                        pai.getTableModel().setValueAt(sanitizado, i, colIndex);
                        String id = (String) pai.getTableModel().getValueAt(i, idIndex);
                        ids.add(id);
                    }
                    pai.atualizarColunaNoBanco(coluna, ids);
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setValue(progressBar.getValue() + 1);
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
        }
        new Thread(() -> {
            executor.shutdown();
            try {
                executor.awaitTermination(5, TimeUnit.MINUTES);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this, "Sanitização concluída com sucesso.");
                statusLabel.setText("Finalizado.");
                dispose();
            });
        }).start();
    }
}
package org.processadorcsv.viewmodel.util;

import org.processadorcsv.ferramentaBD.model.service.DBService;
import org.processadorcsv.ferramentaBD.model.service.IDBService;

import javax.swing.*;
import java.io.File;
import java.util.List;

public class CSVImporterThread implements Runnable {
    private final File arquivoCSV;
    private final String tabela;
    private final IDBService dbService;
    private final Runnable callback;
    public CSVImporterThread(File arquivoCSV, String tabela, IDBService dbService, Runnable callback) {
        this.arquivoCSV = arquivoCSV;
        this.tabela = tabela;
        this.dbService = dbService;
        this.callback = callback;
    }

    @Override
    public void run() {
        try {
            List<String[]> linhas = CsvUtil.lerCSV(arquivoCSV);
            if (linhas.isEmpty()) {
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(null, "O arquivo está vazio.")
                );
                return;
            }
            String[] colunas = linhas.get(0);
            for (int i = 1; i < linhas.size(); i++) {
                dbService.inserirRegistro(tabela, colunas, linhas.get(i));
            }
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(null, "Importação concluída com sucesso!");
                callback.run();
            });
        } catch (Exception ex) {
            SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(null, "Erro na importação: " + ex.getMessage())
            );
        }
    }
    public void iniciar() {
        Thread thread = new Thread(this);
        thread.start();
    }
}
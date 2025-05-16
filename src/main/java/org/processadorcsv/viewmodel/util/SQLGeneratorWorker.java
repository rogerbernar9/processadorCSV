package org.processadorcsv.viewmodel.util;

import javax.swing.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class SQLGeneratorWorker implements Runnable {
    private final BlockingQueue<List<Object>> queue;
    private final int id;
    private final String tableName;
    private final List<String> columnNames;
    private final JCheckBox[] columnChecks;
    private final JTextField[] renamedFields;
    private final JComboBox<String>[] typeSelectors;
    private final JCheckBox[] sanitizeChecks;
    private final JCheckBox[] parseDateChecks;
    private final StringBuilder resultBuilder;
    public SQLGeneratorWorker(int id,
                              BlockingQueue<List<Object>> queue,
                              String tableName,
                              List<String> columnNames,
                              JCheckBox[] columnChecks,
                              JTextField[] renamedFields,
                              JComboBox<String>[] typeSelectors,
                              JCheckBox[] sanitizeChecks,
                              JCheckBox[] parseDateChecks,
                              StringBuilder resultBuilder) {
        this.id = id;
        this.queue = queue;
        this.tableName = tableName;
        this.columnNames = columnNames;
        this.columnChecks = columnChecks;
        this.renamedFields = renamedFields;
        this.typeSelectors = typeSelectors;
        this.sanitizeChecks = sanitizeChecks;
        this.parseDateChecks = parseDateChecks;
        this.resultBuilder = resultBuilder;
    }
    @Override
    public void run() {
        try {
            while (true) {
                List<Object> row = queue.poll(2, TimeUnit.SECONDS);
                if (row == null) break; // encerramento
                StringBuilder sb = new StringBuilder();
                sb.append("INSERT INTO ").append(tableName).append(" (");
                List<Integer> selectedIndexes = new ArrayList<>();
                for (int i = 0; i < columnNames.size(); i++) {
                    if (columnChecks[i] != null && columnChecks[i].isSelected()) {
                        selectedIndexes.add(i);
                        sb.append(renamedFields[i].getText());
                        if (selectedIndexes.size() < countSelected(columnChecks)) {
                            sb.append(", ");
                        }
                    }
                }
                sb.append(") VALUES (");
                for (int i = 0; i < selectedIndexes.size(); i++) {
                    int col = selectedIndexes.get(i);
                    String tipo = (String) typeSelectors[col].getSelectedItem();
                    Object valor = row.get(col);
                    String valorFormatado;
                    if (valor == null) {
                        valorFormatado = "NULL";
                    } else if ("INTEGER".equals(tipo) || "DOUBLE".equals(tipo)) {
                        valorFormatado = valor.toString();
                    } else if ("BOOLEAN".equals(tipo)) {
                        valorFormatado = Boolean.parseBoolean(valor.toString()) ? "TRUE" : "FALSE";
                    } else {
                        String texto = valor.toString();
                        if (sanitizeChecks[col].isSelected()) {
                            texto = texto.replaceAll("[.,\\-*]", "");
                        }
                        if ("DATE".equals(tipo) && parseDateChecks[col].isSelected()) {
                            try {
                                SimpleDateFormat inputFormat = new SimpleDateFormat("dd/MM/yyyy");
                                inputFormat.setLenient(false);
                                SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd");
                                java.util.Date data = inputFormat.parse(texto);
                                texto = outputFormat.format(data);
                            } catch (ParseException ex) {
                                texto = "1970-01-01";
                            }
                        }
                        valorFormatado = "'" + texto.replace("'", "''") + "'";
                    }
                    sb.append(valorFormatado);
                    if (i < selectedIndexes.size() - 1) sb.append(", ");
                }
                sb.append(");\n");
                synchronized (resultBuilder) {
                    resultBuilder.append(sb);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private int countSelected(JCheckBox[] checkboxes) {
        int count = 0;
        for (JCheckBox checkbox : checkboxes) {
            if (checkbox != null && checkbox.isSelected()) {
                count++;
            }
        }
        return count;
    }
}
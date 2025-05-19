package org.processadorcsv.viewmodel.util;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class CSVExporterWorker implements Runnable {

    private final BlockingQueue<List<Object>> queue;
    private final List<String> columnNames;
    private final FileWriter writer;

    public CSVExporterWorker(BlockingQueue<List<Object>> queue, List<String> columnNames, FileWriter writer) {
        this.queue = queue;
        this.columnNames = columnNames;
        this.writer = writer;
    }

    @Override
    public void run() {
        try {
            // writer.write(String.join(",", columnNames) + "\n"); //remove cabecalho intencional

            while (true) {
                List<Object> row = queue.take();
                if (row.isEmpty()) break;
                List<String> linhaFormatada = new ArrayList<>();
                for (Object valor : row) {
                    if (valor == null) {
                        linhaFormatada.add("");
                    } else if (valor instanceof Number) {
                        linhaFormatada.add(String.valueOf(valor));
                    } else {
                        String texto = String.valueOf(valor).replace("\"", "\"\"");
                        linhaFormatada.add("\"" + texto + "\"");
                    }
                }
                writer.write(String.join(",", linhaFormatada) + "\n");
            }
            writer.flush();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

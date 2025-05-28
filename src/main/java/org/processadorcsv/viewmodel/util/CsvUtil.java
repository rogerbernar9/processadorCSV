package org.processadorcsv.viewmodel.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
public class CsvUtil {

    public static List<String[]> lerCSV(File arquivo) throws Exception {
        List<String[]> linhas = new ArrayList<>();
        try (Scanner scanner = new Scanner(arquivo)) {
            while (scanner.hasNextLine()) {
                String linha = scanner.nextLine();
                String[] valores = linha.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                for (int i = 0; i < valores.length; i++) {
                    valores[i] = valores[i].trim().replaceAll("^\"|\"$", ""); // Remove aspas externas
                }
                linhas.add(valores);
            }
        }
        return linhas;
    }

}
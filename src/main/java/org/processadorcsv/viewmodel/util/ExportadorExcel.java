package org.processadorcsv.viewmodel.util;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Vector;

public class ExportadorExcel {
    public static void exportar(Vector<String> columnNames, File outputFile, String dbPath) throws Exception {
        System.out.println("Exportando para: " + outputFile.getAbsolutePath());
        try (HSSFWorkbook workbook = new HSSFWorkbook();
             Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath)) {
            HSSFSheet sheet = workbook.createSheet("Dados CSV");

            HSSFRow headerRow = sheet.createRow(0);
            for (int i = 0; i < columnNames.size(); i++) {
                HSSFCell cell = headerRow.createCell(i);
                cell.setCellValue(columnNames.get(i));
            }

            Statement stmt = conn.createStatement();
            String query = "SELECT " + String.join(", ", columnNames) + " FROM csv_data";
            System.out.println("Executando query: " + query);
            ResultSet rs = stmt.executeQuery(query);
            int rowNum = 1;
            while (rs.next()) {
                HSSFRow row = sheet.createRow(rowNum++);
                for (int i = 0; i < columnNames.size(); i++) {
                    String value = rs.getString(columnNames.get(i));
                    row.createCell(i).setCellValue(value != null ? value : "");
                }
                if (rowNum % 5000 == 0) {
                    System.out.println("Exportados: " + rowNum);
                }
            }

            try (FileOutputStream fileOut = new FileOutputStream(outputFile)) {
                workbook.write(fileOut);
            }
            System.out.println("Arquivo salvo com sucesso.");
        }
    }
}
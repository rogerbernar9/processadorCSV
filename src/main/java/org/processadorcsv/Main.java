package org.processadorcsv;


import org.processadorcsv.viewmodel.CsvReader;
import org.processadorcsv.viewmodel.LoginLDAPView;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        String javaVersion = System.getProperty("java.version");
        String javaHome = System.getProperty("java.home");
        // Imprime no console versão java e caminho jdk
        System.out.println("Executando com Java versão: " + javaVersion);
        System.out.println("Java home (JDK/JRE): " + javaHome);
        try (java.io.PrintWriter out = new java.io.PrintWriter("versao_java.log")) {
            out.println("Executando com Java versão: " + javaVersion);
            out.println("Java home (JDK/JRE): " + javaHome);
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> new CsvReader().setVisible(true));
    }

}
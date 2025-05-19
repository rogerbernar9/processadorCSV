package org.processadorcsv.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Configuracao {
    private static final Properties prop = new Properties();
    static {
        try (InputStream input = Configuracao.class.getClassLoader()
                .getResourceAsStream("environment.properties")) {
            if (input == null) {
                System.err.println("Arquivo 'environment.properties' não encontrado no classpath.");
            } else {
                prop.load(input);
            }
        } catch (IOException ex) {
            System.err.println("Erro ao carregar o arquivo de configuração:");
            ex.printStackTrace();
        }
    }
    public static String get(String chave) {
        return prop.getProperty(chave);
    }
    public static void main(String[] args) {
        String texto = Configuracao.get("VERSAO");
        System.out.println("Versão: " + texto);
    }
}
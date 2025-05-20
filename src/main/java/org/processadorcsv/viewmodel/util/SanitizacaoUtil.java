package org.processadorcsv.viewmodel.util;

public class SanitizacaoUtil {

    public static String removerCaracteresEspeciais(String valor) {
        if (valor == null) return null;
        return valor.replaceAll("[^a-zA-Z0-9]", "");
    }

    public static String removerEspacos(String valor) {
        if (valor == null) return null;
        return valor.replaceAll("\\s+", "");
    }

    public static String paraCaixaAlta(String valor) {
        return valor != null ? valor.toUpperCase() : null;
    }

    public static String preencherCpfComZeros(String valor) {
        if (valor == null) return null;
        String cpfNumerico = valor.replaceAll("\\D", "");
        while (cpfNumerico.length() < 11) {
            cpfNumerico = "0" + cpfNumerico;
        }
        return cpfNumerico;
    }
}
package org.processadorcsv.viewmodel.util;

public class SanitizacaoUtil {
    public static String removerCaracteresEspeciais(String valor) {
        if (valor == null) return null;
        return valor.replaceAll("[^a-zA-Z0-9]", "");
    }
}
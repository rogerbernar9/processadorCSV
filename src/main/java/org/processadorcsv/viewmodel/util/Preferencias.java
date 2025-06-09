package org.processadorcsv.viewmodel.util;

public class Preferencias {
    private static boolean otimizarCarregamento = false;

    public static boolean isOtimizarCarregamento() {
        return otimizarCarregamento;
    }

    public static void setOtimizarCarregamento(boolean valor) {
        otimizarCarregamento = valor;
    }
}

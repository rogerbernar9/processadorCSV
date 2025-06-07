package org.processadorcsv.viewmodel.util;

public class PerformanceTuner {

    public static int getCoresDisponiveis() {
        return Runtime.getRuntime().availableProcessors();
    }

    public static long getMemoriaDisponivelMB() {
        return Runtime.getRuntime().maxMemory() / (1024 * 1024);
    }

    public static int calculaBatchSizeOtimizado() {
        int cores = getCoresDisponiveis();
        long memoryMB = getMemoriaDisponivelMB();
        System.out.println("Memoria RAM:  "+memoryMB);
        System.out.println("Cores CPU: "+cores);

        if (memoryMB < 512) {
            return 100;
        } else if (memoryMB < 2048) {
            return 500;
        } else if (memoryMB < 4096) {
            return 1000;
        } else {
            return 2000;
        }
    }

    public static int calculaThreadsOtimizada() {
        int cores = getCoresDisponiveis();
        return Math.max(1, cores - 1); // um nucleo livre
    }
}

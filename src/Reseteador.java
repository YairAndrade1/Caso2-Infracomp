package src;

import static src.SimuladorMemoria.marcos;

public class Reseteador extends Thread {
    @Override
    public void run() {
        // Mientras no se haya terminado el procesamiento se repite el reseteo
        while (!SimuladorMemoria.procesamientoTerminado.get()) {
            try {
                Thread.sleep(1); // Duerme 1 ms
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            resetearMarcos(); // Se reinicia el bit de referencia de los marcos
        }
    }

    /***
     * Método para resetear los bits de referencia de todos los marcos
     * */
    public static synchronized void resetearMarcos() {
        for (Marco marco : marcos) {
            marco.R = false;
        }
    }
}

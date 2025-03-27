package src;

import static src.SimuladorMemoria.procesarReferenciaAtomica;
import static src.SimuladorMemoria.referencias;

public class Procesador extends Thread {

    @Override
    public void run() {
        int contador = 0;
        // Recorre cada referencia almacenada en la lista
        for (Referencia ref : referencias) {
            // Procesa cada referencia de forma atómica
            procesarReferenciaAtomica(ref);
            contador++;
            // Cada 10,000 referencias se duerme 1ms
            if (contador % 10000 == 0) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}

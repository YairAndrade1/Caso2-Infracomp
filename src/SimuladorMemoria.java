package src;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class SimuladorMemoria {

    // Arreglo de marcos (la "RAM" disponible)
    public static Marco[] marcos;

    // Contadores globales de la simulación
    public static long totalAciertos = 0; // Hits
    public static long totalFallos = 0;   // Misses
    public static long tiempoTotalNS = 0; // Tiempo total simulado en nanosegundos

    // Constantes de tiempo (ns)
    public static final long TIEMPO_ACIERTO_NS = 50;         // 50 ns por acierto
    public static final long TIEMPO_FALLO_NS = 10_000_000;     // 10 ms = 10,000,000 ns por fallo

    // Lista de referencias a procesar
    public static List<Referencia> referencias = new ArrayList<>();

    // Flag atómico para indicar si el procesamiento terminó
    public static AtomicBoolean procesamientoTerminado = new AtomicBoolean(false);

    public static void main(String[] args) {
        // Inicializar variables en 0
        referencias = new ArrayList<>();
        totalAciertos = 0;
        totalFallos = 0;
        tiempoTotalNS = 0;

        Scanner scanner = new Scanner(System.in);
        System.out.print("Ingrese el número de marcos de página: ");
        int numeroMarcos = scanner.nextInt();
        scanner.nextLine();

        String nombreArchivoReferencias = "datos/referencias.txt";

        // Inicializar el arreglo de marcos
        marcos = new Marco[numeroMarcos];
        for (int i = 0; i < numeroMarcos; i++) {
            marcos[i] = new Marco();
        }

        // Lectura del archivo de referencias
        try (BufferedReader br = new BufferedReader(new FileReader(nombreArchivoReferencias))) {
            // Leer la cabecera TP, NF, NC, NR, NP
            String linea = br.readLine(); // TP
            int TP = Integer.parseInt(linea.split("=")[1].trim());
            linea = br.readLine(); // NF
            int NF = Integer.parseInt(linea.split("=")[1].trim());
            linea = br.readLine(); // NC
            int NC = Integer.parseInt(linea.split("=")[1].trim());
            linea = br.readLine(); // NR
            int NR = Integer.parseInt(linea.split("=")[1].trim());
            linea = br.readLine(); // NP
            int NP = Integer.parseInt(linea.split("=")[1].trim());

            System.out.println("Datos de cabecera: TP=" + TP + ", NF=" + NF +
                    ", NC=" + NC + ", NR=" + NR + ", NP=" + NP);

            // Leer todas las referencias y agregarlas a la lista
            String lineaReferencia;
            while ((lineaReferencia = br.readLine()) != null) {
                String[] partes = lineaReferencia.split(",");
                if (partes.length == 4) {
                    String nombre = partes[0].trim();
                    int pagina = Integer.parseInt(partes[1].trim());
                    int offset = Integer.parseInt(partes[2].trim());
                    char tipo = partes[3].trim().charAt(0);
                    referencias.add(new Referencia(nombre, pagina, offset, tipo));
                }
            }
            System.out.println("Se cargaron " + referencias.size() + " referencias.");
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // Crear e iniciar los hilos
        Procesador hiloProcesador = new Procesador();
        Reseteador hiloReseteador = new Reseteador();
        hiloProcesador.start();
        hiloReseteador.start();

        // Esperar a que el procesador termine
        try {
            hiloProcesador.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // Indicar que el procesamiento ha finalizado para que el reseteador se pare
        procesamientoTerminado.set(true);

        // Imprimir los resultados de la simulación
        System.out.println("Simulación terminada.");
        System.out.println("Total de referencias: " + referencias.size());
        System.out.println("Aciertos: " + totalAciertos);
        System.out.println("Fallos: " + totalFallos);
        System.out.println("Tiempo total simulado: " + tiempoTotalNS + " ns");
        double porcentajeAciertos = (totalAciertos * 100.0) / referencias.size();
        System.out.printf("Porcentaje de aciertos: %.4f%%\n", porcentajeAciertos);
        long tiempoHipoteticoAciertos = referencias.size() * TIEMPO_ACIERTO_NS;
        long tiempoHipoteticoFallos = referencias.size() * TIEMPO_FALLO_NS;
        System.out.println("Tiempo si todos fueran aciertos: " + tiempoHipoteticoAciertos + " ns");
        System.out.println("Tiempo si todos fueran fallos: " + tiempoHipoteticoFallos + " ns");
    }

    /***
     * Método para procesar de forma atómica una referencia, garantizando que el acceso a la sección crítica (los marcos de memoria)
     * se realice sin interferencias de otros hilos.
     * @param ref: La referencia que se va a procesar
     * */
    public static void procesarReferenciaAtomica(Referencia ref) {
        int pagina = ref.getPagina();
        char tipoAcceso = ref.getTipoAcceso();
        // Se llama al metodo sincronizado
        boolean esAcierto = procesarReferenciaCritica(pagina, tipoAcceso);

        // Actualizar el tiempo simulado
        if (esAcierto) {
            tiempoTotalNS += TIEMPO_ACIERTO_NS;
        } else {
            tiempoTotalNS += TIEMPO_FALLO_NS;
        }
    }

    /***
     * Método auxiliar para procesar de manera sincronizada la sección critica que son los marcos
     * Revisa si la pagina de la referencia se encuentra en la "RAM", en ese caso, es un hit
     * En caso en que no se encuentre la pagina en la "RAM" se busca un marco que este vacío y se carga la pagina ahi
     * En caso de que no hay ninguna vacia, hay que usar el algoritmo NRU y se utiliza la funcion auxiliar obtenerClaseNRU para encontrar al candidato por el cual remplazar el marco
     * @param pagina: La página donde se encuentra la referencia en la memoria virtual
     * @param tipoAcceso: El tipo de acceso de la referencia W o R
     * @return true si la referencia produjo un hit (la página ya estaba en RAM) o false si fue un miss
     * */
    public static synchronized boolean procesarReferenciaCritica(int pagina, char tipoAcceso) {
        boolean esAcierto = false;
        // Buscar si la página ya está cargada en algún marco
        for (Marco marco : marcos) {
            if (marco.numeroPagina == pagina) {
                // Se entra si la pagina esta en la RAM, es decir un hit
                marco.R = true; // Marcar como usada
                if (tipoAcceso == 'W') {
                    marco.M = true; // Si es escritura, marcar como modificada
                }
                esAcierto = true;
                break;
            }
        }
        if (!esAcierto) {
            // No se encontró la página, es decir miss
            totalFallos++;
            // Se busca un marco libre
            Marco marcoLibre = null;
            for (Marco marco : marcos) {
                if (marco.numeroPagina == -1) {
                    marcoLibre = marco;
                    break;
                }
            }
            if (marcoLibre != null) {
                // Si hay un marco libre entonces se carga la página ahi
                marcoLibre.numeroPagina = pagina;
                marcoLibre.R = true;
                marcoLibre.M = (tipoAcceso == 'W');
            } else {
                // No hay un marco libre, entonces toca usar el algoritmo NRU para elegir un marco a reemplazar
                Marco candidato = null;
                int claseCandidato = 4; // La clase maxima es 3 pero se inicializa en 4 para que se pueda encontrar un mejor candidato
                for (Marco marco : marcos) {
                    int claseActual = obtenerClaseNRU(marco);
                    if (claseActual < claseCandidato) {
                        claseCandidato = claseActual;
                        candidato = marco;
                        // Se selecciona la primera opción si es clase 0 que es la ideal
                        if (claseCandidato == 0) break;
                    }
                }
                if (candidato != null) {
                    candidato.numeroPagina = pagina;
                    candidato.R = true;
                    candidato.M = (tipoAcceso == 'W');
                }
            }
        } else {
            totalAciertos++;
        }
        return esAcierto;
    }
    /***
     * Método para obtener la clase NRU de un marco basándose en los bits R y M
     * Es de clase 0 si el bit de referencia y el de modificación son 0
     * Es de clase 1 si el bit de referencia es 0 y de modificación es 1
     * Es de clase 2 si el bit de referencia es 1 y de modificación es 0
     * Es de clase 3 si el bit de referencia y el de modificación son 1
     * @param marco: El marco al que se le va a calcular la clase
     * @return el valor de la clase entre 0 y 3
     * */
    public static int obtenerClaseNRU(Marco marco) {
        int r = marco.R ? 1 : 0;
        int m = marco.M ? 1 : 0;
        if (r == 0 && m == 0)
            return 0;
        if (r == 0 && m == 1)
            return 1;
        if (r == 1 && m == 0)
            return 2;
        return 3;
    }
}

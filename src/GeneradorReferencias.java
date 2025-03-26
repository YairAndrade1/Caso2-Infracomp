package src;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class GeneradorReferencias {

    public static void main(String[] args) {
        // Se crea el Scanner para leer datos desde la consola
        Scanner sc = new Scanner(System.in);

        // Se pide al usuario que ingrese el tamaño de la página en bytes
        System.out.print("Ingrese el tamaño de página (en bytes): ");
        int pageSize = sc.nextInt();
        sc.nextLine(); // Se consume el salto de línea restante

        // Se define el nombre del archivo de la imagen BMP
        // Para facilitar las pruebas lo manejamos como una variable definida
        String imageFile = "datos/caso2-parrotspeq.bmp";

        // Se crea un objeto Imagen para cargar la imagen BMP
        Imagen imagen = new Imagen(imageFile);
        int NF = imagen.alto;    // Número de filas de la imagen
        int NC = imagen.ancho;   // Número de columnas de la imagen

        // Cálculo de las bases y tamaños de cada estructura en memoria virtual

        // Se asume que la imagen comienza en la dirección 0
        int baseImagen = 0;
        // Cada pixel tiene 3 bytes para los canales R, G y B
        int sizeImagen = NF * NC * 3;

        // La estructura SOBEL_X va después de la imagen
        int baseSobelX = baseImagen + sizeImagen;
        // El filtro Sobel es una matriz de 3x3 y cada elemento es un entero (Ósea que ocupa 4 bytes)
        int sizeSobel = 3 * 3 * 4;

        // La estructura SOBEL_Y se ubica después de SOBEL_X
        int baseSobelY = baseSobelX + sizeSobel;
        // El tamaño de SOBEL_Y es el mismo que el de SOBEL_X
        int sizeSobelY = sizeSobel;

        // La estructura Rta va después de SOBEL_Y
        int baseRta = baseSobelY + sizeSobelY;
        // La matriz Rta tiene el mismo tamaño que la imagen
        int sizeRta = sizeImagen;


        // Cálculo del número total de páginas virtuales (NP)

        // Se calcula la cantidad total de bytes que usa el proceso
        int totalBytes = baseRta + sizeRta;
        // Se divide el total de bytes por el tamaño de página para obtener el número de página redondeando hacia arriba.
        int NP = (totalBytes + pageSize - 1) / pageSize;

        // Se crea una lista para almacenar todas las referencias generadas
        List<Referencia> referencias = new ArrayList<>();

        // Generación de las referencias

        // Se recorre cada píxel interior (desde la fila 1 hasta NF-2, y de la columna 1 hasta NC-2, ya que los bordes no tiene vecinos válidos para aplicar el filtro)
        for (int i = 1; i < NF - 1; i++) {
            for (int j = 1; j < NC - 1; j++) {
                // Se recorre cada vecino del píxel actual formando un bloque 3x3
                for (int ki = -1; ki <= 1; ki++) {
                    for (int kj = -1; kj <= 1; kj++) {
                        // Se determina la fila y columna del vecino en la imagen
                        int rowImg = i + ki;
                        int colImg = j + kj;

                        // Se realizan 3 accesos para cada canal (r, g y b)
                        for (int channel = 0; channel < 3; channel++) {
                            // Se calcula el offset dentro de la matriz imagen
                            // Cada fila tiene (NC * 3) bytes, y cada pixel ocupa 3 bytes
                            int offsetImagen = (rowImg * NC * 3) + (colImg * 3) + channel;
                            // Se suma la base de la imagen para obtener el offset absoluto
                            int absOffset = baseImagen + offsetImagen;
                            // Se determina el número de página donde se encuentra el dato
                            int page = absOffset / pageSize;
                            // Y el offset dentro de la página
                            int offsetInPage = absOffset % pageSize;
                            // Se asigna un nombre indicando el canal accedido
                            String channelChar = (channel == 0) ? "r" : (channel == 1 ? "g" : "b");
                            String nombre = "Imagen[" + rowImg + "][" + colImg + "]." + channelChar;
                            // Se crea y añade la referencia de lectura a la lista
                            referencias.add(new Referencia(nombre, page, offsetInPage, 'R'));
                        }

                        // Se realizan 3 accesos para cada celda del filtro SOBEL_X
                        // Se calcula la posición en la matriz 3x3
                        int indexSobel = ((ki + 1) * 3) + (kj + 1);
                        // Cada entero ocupa 4 bytes
                        int offsetSobelX = indexSobel * 4;
                        // Se obtiene el offset absoluto en la estructura SOBEL_X
                        int absOffsetSobelX = baseSobelX + offsetSobelX;
                        for (int k = 0; k < 3; k++) {
                            int page = absOffsetSobelX / pageSize;
                            int offsetInPage = absOffsetSobelX % pageSize;
                            // Se asigna un nombre para indicar la posición en el filtro
                            String nombre = "SOBEL_X[" + (ki + 1) + "][" + (kj + 1) + "]";
                            // Se añade la referencia de lectura
                            referencias.add(new Referencia(nombre, page, offsetInPage, 'R'));
                        }

                        // Similar a SOBEL_X
                        // Se realizan 3 accesos para cada celda del filtro SOBEL_Y
                        int offsetSobelY = indexSobel * 4;
                        int absOffsetSobelY = baseSobelY + offsetSobelY;
                        for (int k = 0; k < 3; k++) {
                            int page = absOffsetSobelY / pageSize;
                            int offsetInPage = absOffsetSobelY % pageSize;
                            String nombre = "SOBEL_Y[" + (ki + 1) + "][" + (kj + 1) + "]";
                            referencias.add(new Referencia(nombre, page, offsetInPage, 'R'));
                        }
                    }
                }

                // Se realizan 3 accesos de escritura (uno por canal: r, g, b) para el píxel actual
                for (int channel = 0; channel < 3; channel++) {
                    int offsetRta = (i * NC * 3) + (j * 3) + channel;
                    int absOffsetRta = baseRta + offsetRta;
                    int page = absOffsetRta / pageSize;
                    int offsetInPage = absOffsetRta % pageSize;
                    String channelChar = (channel == 0) ? "r" : (channel == 1 ? "g" : "b");
                    String nombre = "Rta[" + i + "][" + j + "]." + channelChar;
                    // Aquí se crea la referencia de escritura y se añade a la lista
                    referencias.add(new Referencia(nombre, page, offsetInPage, 'W'));
                }
            }
        }

        // Escritura de las referencias en un archivo de salida

        try {
            // Se crea un writer para escribir en el archivo "datos/referencias.txt"
            PrintWriter writer = new PrintWriter(new FileWriter("datos/referencias.txt"));
            // Se escribe la cabecera con los parámetros generales
            writer.println("TP=" + pageSize);
            writer.println("NF=" + NF);
            writer.println("NC=" + NC);
            writer.println("NR=" + referencias.size());
            writer.println("NP=" + NP);
            // Se escribe cada referencia en una línea
            for (Referencia ref : referencias) {
                writer.println(ref.toString());
            }
            // Se cierra el writer
            writer.close();
            System.out.println("Archivo de referencias generado con " + referencias.size() + " referencias.");
        } catch (IOException e) {
            // Si ocurre algún error durante la escritura del archivo, se imprime la traza del error
            e.printStackTrace();
        }
    }
}

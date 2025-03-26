package src;

import java.util.Scanner;

public class MenuPrincipal {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int opcion;
        do {
            System.out.println("------ MENU PRINCIPAL ------");
            System.out.println("1. Generar Referencias (Opción 1)");
            System.out.println("2. Simular Memoria (Opción 2)");
            System.out.println("0. Salir");
            System.out.print("Seleccione una opción: ");
            opcion = sc.nextInt();
            sc.nextLine(); //

            switch (opcion) {
                case 1:
                    // Llama a la opción 1
                    GeneradorReferencias.main(args);
                    break;
                case 2:
                    // Llama a la opción 2
                    SimuladorMemoria.main(args);
                    break;
                case 0:
                    System.out.println("Saliendo del programa.");
                    break;
                default:
                    System.out.println("Opción no válida. Intente de nuevo.");
            }
        } while (opcion != 0);
        sc.close();
    }
}

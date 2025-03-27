package src;

public class Marco {
    int numeroPagina; // Número de la página cargada (-1 indica el marco vacío)
    boolean R;        // Bit de referencia que indica que se usó recientemente
    boolean M;        // Bit de modificación que indica que se escribió en la página

    public Marco() {
        numeroPagina = -1;
        R = false;
        M = false;
    }
}

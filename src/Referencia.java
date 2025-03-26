package src;

public class Referencia {
    private String nombre;  // ej: Imagen[0][1].g
    private int pagina;
    private int offset;
    private char tipoAcceso; // 'R' o 'W'

    public Referencia(String nombre, int pagina, int offset, char tipoAcceso) {
        this.nombre = nombre;
        this.pagina = pagina;
        this.offset = offset;
        this.tipoAcceso = tipoAcceso;
    }

    public String getNombre() {
        return nombre;
    }

    public int getPagina() {
        return pagina;
    }

    public int getOffset() {
        return offset;
    }

    public char getTipoAcceso() {
        return tipoAcceso;
    }

    @Override
    public String toString() {
        return nombre + "," + pagina + "," + offset + "," + tipoAcceso;
    }
}

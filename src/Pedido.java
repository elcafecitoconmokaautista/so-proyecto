// Pedido.java - el modelo de la semana 3 (reconstruido). Formato de linea: ID;PRODUCTO;CANTIDAD
public class Pedido {

    private final int id;
    private final String producto;
    private final int cantidad;
    private volatile String estado = "PENDIENTE";   // PENDIENTE | CONFIRMADO | RECHAZADO

    public Pedido(int id, String producto, int cantidad) {
        this.id = id;
        this.producto = producto;
        this.cantidad = cantidad;
    }

    // Parte "ID;PRODUCTO;CANTIDAD" y construye el objeto.
    public static Pedido desdeLinea(String linea) {
        String[] p = linea.trim().split(";");
        if (p.length != 3) throw new IllegalArgumentException("Formato invalido: " + linea);
        try {
            return new Pedido(Integer.parseInt(p[0].trim()),
                              p[1].trim().toLowerCase(),
                              Integer.parseInt(p[2].trim()));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Numero invalido en: " + linea);
        }
    }

    // Sirve para descartar lineas como "Generador arrancado" que tambien viajan por la tuberia.
    public static boolean esLineaDePedido(String linea) {
        return linea.matches("\\s*\\d+;[^;]+;\\d+\\s*");
    }

    public int getId() { return id; }
    public String getProducto() { return producto; }
    public int getCantidad() { return cantidad; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    @Override
    public String toString() {
        return "Pedido[id=" + id + ", producto=" + producto
             + ", cantidad=" + cantidad + ", estado=" + estado + "]";
    }
}

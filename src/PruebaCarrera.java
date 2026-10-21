// PruebaCarrera.java - N cajeros peleando por el mismo producto.
// Uso: java PruebaCarrera [cajeros] [existenciaInicial]     (por omision 10 y 10)
public class PruebaCarrera {

    public static void main(String[] args) throws InterruptedException {

        // TODO: cambia por un producto de TU catalogo
        String producto = "paracetamol";
        int cajeros = args.length > 0 ? Integer.parseInt(args[0]) : 10;
        int existenciaInicial = args.length > 1 ? Integer.parseInt(args[1]) : 10;

        GestorInventario inventario = new GestorInventario();
        inventario.agregar(producto, existenciaInicial);

        Thread[] hilos = new Thread[cajeros];
        for (int i = 0; i < cajeros; i++) {
            final int numero = i + 1;
            hilos[i] = new Thread(() -> {
                boolean vendido = inventario.vender(producto, 1);
                System.out.println("Cajero " + numero + ": " + (vendido ? "VENDIDO" : "rechazado"));
            });
        }

        for (Thread h : hilos) h.start();
        for (Thread h : hilos) h.join();       // sin join, el resumen saldria antes de que acaben

        System.out.println("---------------------------------------");
        System.out.println("Existencia inicial:   " + existenciaInicial);
        System.out.println("Cajeros:              " + cajeros);
        System.out.println("Ventas confirmadas:   " + inventario.getVentasConfirmadas());
        System.out.println("Ventas rechazadas:    " + inventario.getVentasRechazadas());
        System.out.println("Existencia final:     " + inventario.existencia(producto));
        System.out.println("---------------------------------------");
        System.out.println("CUADRA? inicial - confirmadas == final ?  "
                + ((existenciaInicial - inventario.getVentasConfirmadas())
                    == inventario.existencia(producto)));
    }
}

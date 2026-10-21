// PruebaRendimiento.java (S6, bloque extra) - cuanto tarda en atender N ventas.
// Uso: java -Dpausa=false PruebaRendimiento 8
public class PruebaRendimiento {

    public static void main(String[] args) throws InterruptedException {

        int cajeros = Integer.parseInt(args[0]);
        int ventasPorCajero = 1000;

        GestorInventario inv = new GestorInventario();
        inv.agregar("prueba", cajeros * ventasPorCajero);

        Thread[] hilos = new Thread[cajeros];
        for (int i = 0; i < cajeros; i++) {
            hilos[i] = new Thread(() -> {
                for (int j = 0; j < ventasPorCajero; j++) inv.vender("prueba", 1);
            });
        }

        long inicio = System.nanoTime();
        for (Thread h : hilos) h.start();
        for (Thread h : hilos) h.join();
        long fin = System.nanoTime();

        long ms = (fin - inicio) / 1_000_000;
        int total = cajeros * ventasPorCajero;
        System.out.println("hilos=" + cajeros + "  ventas=" + total + "  tiempo=" + ms + " ms"
                + "  throughput=" + (total * 1000L / Math.max(ms, 1)) + " ventas/s");
    }
}

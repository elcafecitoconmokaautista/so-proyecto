// PruebaBloqueo.java (S7) - dos hilos que se abrazan hasta morir.
// A PROPOSITO el cajero-2 pide los candados en orden INVERTIDO: esto es evidencia, no un error.
// Uso:  java PruebaBloqueo            -> se traba (diagnostica con jstack PID)
//       java PruebaBloqueo ordenado   -> los dos piden en el mismo orden (INVENTARIO=1, LOG=2) y terminan
public class PruebaBloqueo {

    // TODO: renombra segun TU dominio (aqui: inventario y log)
    static final Object INVENTARIO = new Object();   // recurso 1
    static final Object LOG = new Object();          // recurso 2

    public static void main(String[] args) {

        boolean ordenado = args.length > 0 && args[0].equals("ordenado");
        System.out.println("PID " + ProcessHandle.current().pid()
                + (ordenado ? "  (modo ordenado: no debe trabarse)" : "  (modo cruzado: se va a trabar)"));

        Thread cajero1 = new Thread(() -> {
            synchronized (INVENTARIO) {
                System.out.println("cajero-1: tomo INVENTARIO");
                dormir(500);                       // le doy tiempo al otro para que tome su candado
                System.out.println("cajero-1: quiero LOG");
                synchronized (LOG) {
                    System.out.println("cajero-1: tengo los dos");
                }
            }
        }, "cajero-1");

        Thread cajero2 = new Thread(() -> {
            Object primero = ordenado ? INVENTARIO : LOG;      // orden invertido a proposito
            Object segundo = ordenado ? LOG : INVENTARIO;
            String np = ordenado ? "INVENTARIO" : "LOG";
            String ns = ordenado ? "LOG" : "INVENTARIO";
            synchronized (primero) {
                System.out.println("cajero-2: tomo " + np);
                dormir(500);
                System.out.println("cajero-2: quiero " + ns);
                synchronized (segundo) {
                    System.out.println("cajero-2: tengo los dos");
                }
            }
        }, "cajero-2");

        cajero1.start();
        cajero2.start();
        System.out.println("Los dos hilos arrancaron. Si no ves 'tengo los dos' dos veces, hay bloqueo.");
    }

    static void dormir(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { }
    }
}

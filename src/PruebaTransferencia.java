// PruebaTransferencia.java (S7, clase) - el interbloqueo "realista": el orden lo ponen los DATOS.
// Un candado por producto + transferencias en sentidos opuestos.
// Uso: java PruebaTransferencia           -> se traba
//      java PruebaTransferencia ordenado  -> siempre se pide primero el candado del nombre menor
import java.util.HashMap;
import java.util.Map;

public class PruebaTransferencia {

    static final Map<String, Object> candados = new HashMap<>();
    static final Map<String, Integer> existencias = new HashMap<>();
    static boolean ordenado;

    static void transferir(String desde, String hacia, int cantidad) {
        String primero = desde, segundo = hacia;
        if (ordenado && desde.compareTo(hacia) > 0) { primero = hacia; segundo = desde; }

        synchronized (candados.get(primero)) {
            dormir(200);                                  // ensancha la ventana
            synchronized (candados.get(segundo)) {
                existencias.put(desde, existencias.get(desde) - cantidad);
                existencias.put(hacia, existencias.get(hacia) + cantidad);
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        ordenado = args.length > 0 && args[0].equals("ordenado");
        System.out.println("PID " + ProcessHandle.current().pid() + (ordenado ? " (ordenado)" : " (cruzado)"));

        for (String p : new String[] {"paracetamol", "ibuprofeno"}) {
            candados.put(p, new Object());
            existencias.put(p, 100);
        }

        Thread a = new Thread(() -> transferir("paracetamol", "ibuprofeno", 5), "transfer-1");
        Thread b = new Thread(() -> transferir("ibuprofeno", "paracetamol", 3), "transfer-2");
        a.start(); b.start();
        a.join(); b.join();        // si hay bloqueo, el main tambien se queda aqui
        System.out.println("Listo: " + existencias);
    }

    static void dormir(long ms) { try { Thread.sleep(ms); } catch (InterruptedException e) { } }
}

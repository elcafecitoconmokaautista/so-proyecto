// GestorInventario.java - inventario compartido, version FINAL (semana 6): con exclusion mutua.
// Lleva la cuenta de lo vendido por producto para poder auditar que todo cuadre.
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GestorInventario {

    // Pausa de 1 ms dentro de la seccion critica. Con el candado bien puesto el resultado
    // debe ser correcto de todas formas. Para medir rendimiento: java -Dpausa=false ...
    private static final boolean PAUSA =
        Boolean.parseBoolean(System.getProperty("pausa", "true"));

    private final Map<String, Integer> existencias = new HashMap<>();
    private final Map<String, Integer> inicial = new HashMap<>();
    private final Map<String, Integer> vendidas = new HashMap<>();
    private int ventasConfirmadas = 0;
    private int ventasRechazadas = 0;

    public synchronized void agregar(String producto, int cantidad) {
        existencias.put(producto, cantidad);
        inicial.put(producto, cantidad);
        vendidas.put(producto, 0);
    }

    // SECCION CRITICA COMPLETA: la consulta (get + if) y el descuento (put)
    // van bajo el MISMO candado. Si solo protegieras el put, seguiria fallando.
    public synchronized boolean vender(String producto, int cantidad) {

        Integer disponible = existencias.get(producto);
        if (disponible == null) {
            ventasRechazadas++;
            return false;
        }

        if (disponible >= cantidad) {
            if (PAUSA) {
                try { Thread.sleep(1); } catch (InterruptedException e) { }
            }
            existencias.put(producto, disponible - cantidad);
            vendidas.put(producto, vendidas.get(producto) + cantidad);
            ventasConfirmadas++;
            return true;
        }

        ventasRechazadas++;
        return false;
    }

    public synchronized int existencia(String producto) {
        return existencias.getOrDefault(producto, 0);
    }

    public synchronized int getVentasConfirmadas() { return ventasConfirmadas; }
    public synchronized int getVentasRechazadas() { return ventasRechazadas; }

    // Lee datos/catalogo.txt con el formato ID;NOMBRE;PRECIO;EXISTENCIA.
    // NO lleva synchronized propio: se llama al arrancar, cuando solo existe el hilo main,
    // y cada alta pasa por agregar(), que si esta protegido.
    public void cargarDesde(String archivo) throws IOException {
        for (String linea : Files.readAllLines(Paths.get(archivo))) {
            linea = linea.trim();
            if (linea.isEmpty() || linea.startsWith("#")) continue;
            String[] c = linea.split(";");
            if (c.length != 4) continue;
            agregar(c[1].trim().toLowerCase(), Integer.parseInt(c[3].trim()));
        }
    }

    // Auditoria: por producto, inicial - vendidas == final ?
    public synchronized List<String> resumen() {
        List<String> lineas = new ArrayList<>();
        for (String p : new java.util.TreeSet<>(existencias.keySet())) {
            int ini = inicial.get(p), ven = vendidas.get(p), fin = existencias.get(p);
            lineas.add(String.format("  %-12s inicial=%-4d vendidas=%-4d final=%-4d cuadra=%s",
                                     p, ini, ven, fin, (ini - ven) == fin));
        }
        return lineas;
    }
}

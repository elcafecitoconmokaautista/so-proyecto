// ContadorCarrera.java (S4, "la grieta") - sumar 1 muchas veces desde muchos hilos NO da el total.
// Uso: java ContadorCarrera [hilos] [sumasPorHilo]     (por omision 1000 y 1000 -> esperado 1000000)
// En una PC con varios nucleos suele fallar solo; en una con 1 nucleo ensancha la ventana: java -Dventana=true ContadorCarrera
// Antes de correrlo, escribe en tu bitacora que numero esperas. Corre 5 veces y anota los 5 resultados.
public class ContadorCarrera {

    static int piezasVendidas = 0;          // variable compartida, SIN proteger a proposito
    static final boolean VENTANA = Boolean.getBoolean("ventana");   // cede el procesador entre LEER y ESCRIBIR

    public static void main(String[] args) throws InterruptedException {
        int hilos = args.length > 0 ? Integer.parseInt(args[0]) : 1000;
        int sumas = args.length > 1 ? Integer.parseInt(args[1]) : 1000;

        Thread[] t = new Thread[hilos];
        for (int i = 0; i < hilos; i++) {
            t[i] = new Thread(() -> {
                for (int j = 0; j < sumas; j++) {
                    int leido = piezasVendidas;            // 1. LEER
                    if (VENTANA) Thread.yield();           //    (el SO puede interrumpir aqui)
                    piezasVendidas = leido + 1;            // 2. SUMAR y 3. ESCRIBIR: tres pasos, no uno
                }
            });
        }
        for (Thread h : t) h.start();
        for (Thread h : t) h.join();

        System.out.println("Esperado: " + (long) hilos * sumas + "   Obtenido: " + piezasVendidas
                + "   Perdidos: " + ((long) hilos * sumas - piezasVendidas));
    }
}

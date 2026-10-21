// ServidorPedidos.java - semanas 4 a 8 en un solo servidor:
//   S4  un hilo por pedido (o pool con -Dpool=N)      S5/S6  inventario compartido y protegido
//   S6  Semaphore opcional (-Dventanillas=N)           S8  apagado ordenado + tuberias
//
// Uso:
//   java ServidorPedidos                       -> lee pedidos de la consola (ID;PRODUCTO;CANTIDAD), "salir" termina
//   java GeneradorPedidos 20 | java ServidorPedidos
//   java ServidorPedidos --generador 20        -> lanza al generador como hijo y lee su salida por tuberia
//   java ServidorPedidos --fifo datos/cajero.pipe  -> escucha una tuberia con nombre (bloque extra S8)
// Opciones -D: trabajo=2000 (ms por pedido), pool=0, ventanillas=0, pausa=true
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ServidorPedidos {

    // TODO: el nombre de tu negocio
    static final String NEGOCIO = "Farmacia SaludYa";

    static final long TRABAJO_MS = Long.getLong("trabajo", 2000L);
    static final int TAM_POOL = Integer.getInteger("pool", 0);
    static final int VENTANILLAS = Integer.getInteger("ventanillas", 0);
    static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    static final GestorInventario inventario = new GestorInventario();
    static final AtomicInteger recibidos = new AtomicInteger();
    static final List<Thread> hilos = Collections.synchronizedList(new ArrayList<>());
    static ExecutorService pool;          // solo si -Dpool=N
    static Semaphore ventanillas;         // solo si -Dventanillas=N

    // volatile: los hilos tienen que VER el cambio que hace el hook de apagado.
    static volatile boolean apagando = false;

    // Contador de la semana 4. DELIBERADAMENTE sin proteger: es la "grieta" que se explica en S5.
    // (No afecta al inventario. Para arreglarlo: AtomicInteger, o synchronized.)
    static int piezasVendidas = 0;

    public static void main(String[] args) throws Exception {

        registrar("Servidor de " + NEGOCIO + " listo. PID " + ProcessHandle.current().pid());
        cargarCatalogo();
        if (TAM_POOL > 0) pool = Executors.newFixedThreadPool(TAM_POOL);
        if (VENTANILLAS > 0) ventanillas = new Semaphore(VENTANILLAS);

        // S8: se ejecuta con SIGINT, SIGTERM y System.exit(). NO con SIGKILL.
        Runtime.getRuntime().addShutdownHook(new Thread(ServidorPedidos::apagadoOrdenado, "apagado"));

        if (args.length >= 2 && args[0].equals("--generador")) {
            recibirDelGenerador(Integer.parseInt(args[1]));
        } else if (args.length >= 2 && args[0].equals("--fifo")) {
            escucharFifo(args[1]);
        } else {
            registrar("Escribe pedidos con el formato ID;PRODUCTO;CANTIDAD ('salir' termina)");
            leer(new BufferedReader(new InputStreamReader(System.in)));
        }
        registrar("Fin de la entrada. Esperando a los pedidos en curso...");
        // Con pool, los hilos del pool mantienen vivo el proceso: hay que avisarles que no vendra mas trabajo.
        if (pool != null) pool.shutdown();
    }

    // ---------- entrada de pedidos ----------

    // Devuelve true si se pidio "salir". El while NO espera a nadie: despacha y sigue leyendo.
    static boolean leer(BufferedReader fuente) throws IOException {
        String linea;
        while (!apagando && (linea = fuente.readLine()) != null) {
            linea = linea.trim();
            if (linea.isEmpty()) continue;
            if (linea.equals("salir")) return true;
            if (!Pedido.esLineaDePedido(linea)) {
                registrar("Ignorada (no es un pedido): " + linea);
                continue;
            }
            int numero = recibidos.incrementAndGet();
            final String pedido = linea;
            despachar(() -> atender(pedido, numero), "pedido-" + numero);
            registrar("Recibido el pedido " + numero + ". Hilos vivos: " + Thread.activeCount());
        }
        return false;
    }

    // S4: un hilo por pedido, o un pool acotado si se pidio -Dpool=N.
    static void despachar(Runnable tarea, String nombre) {
        if (pool != null) {
            try { pool.submit(tarea); } catch (RejectedExecutionException e) { /* apagando */ }
            return;
        }
        Thread hilo = new Thread(tarea, nombre);   // new Thread NO arranca nada...
        hilos.add(hilo);
        hilo.start();                              // ...start() si. (run() seria secuencial)
    }

    // S8 parte B: la salida del hijo ES la tuberia que creo el kernel.
    static void recibirDelGenerador(int cuantos) throws Exception {
        String java = ProcessHandle.current().info().command().orElse("java");
        ProcessBuilder pb = new ProcessBuilder(java, "-cp", System.getProperty("java.class.path"),
                                               "GeneradorPedidos", String.valueOf(cuantos));
        pb.redirectErrorStream(true);
        Process hijo = pb.start();
        registrar("Generador lanzado con PID " + hijo.pid());

        try (BufferedReader tuberia = new BufferedReader(new InputStreamReader(hijo.getInputStream()))) {
            leer(tuberia);   // las lineas "arrancado"/"termina" las descarta esLineaDePedido
        }
        if (apagando) hijo.destroy();
        int salida = hijo.waitFor();
        registrar("El generador termino con codigo " + salida);
    }

    // S8 extra A: FIFO. Abrir para leer se bloquea hasta que alguien abra para escribir;
    // cuando el escritor cierra llega fin de archivo, y se vuelve a abrir para el siguiente cajero.
    static void escucharFifo(String ruta) throws IOException {
        registrar("Escuchando la tuberia con nombre " + ruta);
        while (!apagando) {
            try (BufferedReader fifo = new BufferedReader(new FileReader(ruta))) {
                if (leer(fifo)) break;
            }
        }
    }

    // ---------- atencion de un pedido (la ejecuta CADA hilo) ----------

    static void atender(String linea, int numero) {
        String hilo = Thread.currentThread().getName();
        registrar(hilo + " empieza a atender: " + linea);

        Pedido pedido;
        try {
            pedido = Pedido.desdeLinea(linea);
        } catch (IllegalArgumentException e) {
            registrar(hilo + " pedido invalido: " + e.getMessage());
            return;
        }

        boolean tomoVentanilla = false;
        try {
            if (ventanillas != null) {                       // S6: Semaphore(N)
                ventanillas.acquire();
                tomoVentanilla = true;
                registrar(hilo + " entro a ventanilla, libres: " + ventanillas.availablePermits());
            }

            Thread.sleep(TRABAJO_MS);                        // simula consultar, cobrar, imprimir

            boolean ok = inventario.vender(pedido.getProducto(), pedido.getCantidad());
            pedido.setEstado(ok ? "CONFIRMADO" : "RECHAZADO");
            if (ok) piezasVendidas = piezasVendidas + pedido.getCantidad();   // carrera deliberada

        } catch (InterruptedException e) {
            registrar(hilo + " fue interrumpido");
            Thread.currentThread().interrupt();
            return;
        } finally {
            if (tomoVentanilla) ventanillas.release();       // SIEMPRE en finally
        }

        registrar(pedido.toString());
        registrar(hilo + " termino");
    }

    // ---------- apagado ordenado (S8) ----------

    static void apagadoOrdenado() {
        apagando = true;
        registrar("Senal de apagado recibida. No se aceptan pedidos nuevos.");

        // 1. Terminar lo que ya empezo (con tope de 10 s: un hook lento = servidor que no se apaga).
        if (pool != null) {
            pool.shutdown();
            try { pool.awaitTermination(10, TimeUnit.SECONDS); } catch (InterruptedException e) { }
        } else {
            List<Thread> copia;
            synchronized (hilos) { copia = new ArrayList<>(hilos); }
            long limite = System.currentTimeMillis() + 10_000;
            for (Thread h : copia) {
                long resto = limite - System.currentTimeMillis();
                if (resto <= 0) break;
                try { h.join(resto); } catch (InterruptedException e) { break; }
            }
        }

        // 2. Resumen final.
        registrar("Resumen final:");
        registrar("  Pedidos recibidos:  " + recibidos.get());
        registrar("  Ventas confirmadas: " + inventario.getVentasConfirmadas());
        registrar("  Ventas rechazadas:  " + inventario.getVentasRechazadas());
        registrar("  Piezas segun el contador SIN proteger (demo S4/S5): " + piezasVendidas);
        for (String l : inventario.resumen()) registrar(l);

        // 3. TODO (semana 13): aqui se cerrarian archivos abiertos y se escribiria lo pendiente.
        registrar("Servidor apagado ordenadamente.");
    }

    // ---------- utilidades ----------

    static void cargarCatalogo() {
        // Se busca desde la raiz del proyecto o desde src/ (como se corre en clase).
        for (String ruta : new String[] {"datos/catalogo.txt", "../datos/catalogo.txt"}) {
            if (Files.exists(Paths.get(ruta))) {
                try {
                    inventario.cargarDesde(ruta);
                    registrar("Catalogo cargado desde " + ruta);
                    return;
                } catch (IOException | RuntimeException e) {
                    registrar("No pude leer " + ruta + ": " + e);
                }
            }
        }
        registrar("AVISO: no encontre datos/catalogo.txt, uso un catalogo de emergencia");
        inventario.agregar("paracetamol", 40);
        inventario.agregar("ibuprofeno", 40);
    }

    static void registrar(String mensaje) {
        System.out.println("[" + LocalTime.now().format(HORA) + "] " + mensaje);
    }
}

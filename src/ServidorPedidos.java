import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Scanner;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit; // Necesario para los tiempos de espera

public class ServidorPedidos {

    // Nombre del negocio
    static final String NEGOCIO = "FarmaciaYa";
    static int piezasVendidas = 0;
    
    // Variables movidas a nivel de clase para que métodos y el ShutdownHook accedan a ellas
    static volatile boolean apagando = false;
    static int contador = 0;
    static Scanner entrada;
    static ExecutorService pool; // El pool ahora es estático para usarlo en cualquier método

    static class Pedido {
        private String id;
        private String producto;
        private int cantidad;

        public Pedido(String id, String producto, int cantidad) {
            this.id = id;
            this.producto = producto;
            this.cantidad = cantidad;
        }

        @Override
        public String toString() {
            return "Pedido { ID: " + id + " | Producto: " + producto + " | Cantidad: " + cantidad + " }";
        }
    }

    public static void main(String[] args) {

        long pid = ProcessHandle.current().pid();
        registrar("Servidor de " + NEGOCIO + " listo. PID " + pid);
        registrar("Escribe pedidos con el formato ID;PRODUCTO;CANTIDAD");

        entrada = new Scanner(System.in);

        // Instanciar el pool de hilos fijo antes del ciclo.
        pool = Executors.newFixedThreadPool(4);

        // Registro del mecanismo de apagado ordenado
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {

            apagando = true;
            registrar("Señal de apagado recibida. No se aceptan pedidos nuevos.");

            // Drenar operaciones en curso
            if (pool != null) {
                pool.shutdown(); // Deja de aceptar tareas nuevas
                try {
                    // Espera hasta 10 segundos a que terminen los hilos actuales
                    if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
                        registrar("Tiempo de espera agotado. Forzando cierre...");
                        pool.shutdownNow(); 
                    }
                } catch (InterruptedException e) {
                    pool.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }

            registrar("Resumen final:");
            registrar("  Pedidos recibidos:  " + contador);
            registrar("  Piezas vendidas:    " + piezasVendidas);

            // Cierre de recursos y archivos abiertos
            if (entrada != null) {
                entrada.close();
            }
            
            registrar("Servidor apagado ordenadamente.");
        }, "apagado"));

        // El ciclo principal verifica que no estemos en proceso de apagado
        while (!apagando && entrada.hasNextLine()) {

            String linea = entrada.nextLine().trim();
            if (linea.isEmpty()) continue;
            if (linea.equals("bye")) break;

            contador++;
            final String pedido = linea;
            final int numero = contador;

            // Enviamos la tarea al pool en lugar de crear un hilo manualmente
            pool.submit(() -> atender(pedido, numero));

            registrar("Recibido el pedido " + numero + ". Hilos vivos: "
                      + Thread.activeCount());
        }

        if (!apagando) {
            registrar("Pedidos recibidos: " + contador);
            registrar("Piezas vendidas segun el contador: " + piezasVendidas);
            registrar("Servidor detenido tras " + contador + " pedidos");
            
            // Apagado limpio del pool si se sale por "bye" o fin de archivo (Ctrl+D)
            pool.shutdown();
            if (entrada != null) {
                entrada.close();
            }
        }
    }

    // Lanza al generador como hijo y lee sus pedidos por la tubería.
    static void recibirDelGenerador(int cuantos) throws Exception {

        ProcessBuilder pb = new ProcessBuilder("java", "GeneradorPedidos",
                                               String.valueOf(cuantos));
        pb.redirectErrorStream(true);

        Process hijo = pb.start();
        registrar("Generador lanzado con PID " + hijo.pid());

        // ESTO es la tubería: la salida del hijo es un flujo que yo leo.
        try (BufferedReader tuberia = new BufferedReader(
                 new InputStreamReader(hijo.getInputStream()))) {

            String linea;
            while ((linea = tuberia.readLine()) != null) {
                
                // FILTRO (Solución al TODO)
                
                if (linea.contains(";")) {
                    contador++;
                    final String pedido = linea.trim();
                    final int numero = contador;
                    
                    // Enviamos al pool, tal como en el main
                    pool.submit(() -> atender(pedido, numero));
                } else {
                    // Si no tiene comas, es un mensaje de estado ("arrancado", "termina", etc.)
                    registrar("Mensaje del generador: " + linea);
                }
            }
        }

        int salida = hijo.waitFor();
        registrar("El generador terminó con código " + salida);
    }

    static void atender(String linea, int numero) {

        String nombreHilo = Thread.currentThread().getName();
        registrar(nombreHilo + " empieza a atender: " + linea);

        String[] partes = linea.split(";");
        Pedido pedidoActual = null;
        
        if (partes.length == 3) {
            try {
                String id = partes[0].trim();
                String producto = partes[1].trim();
                int cantidad = Integer.parseInt(partes[2].trim());
                pedidoActual = new Pedido(id, producto, cantidad);
            } catch (NumberFormatException e) {
                registrar(nombreHilo + " ERROR: La cantidad debe ser un número entero.");
                return;
            }
        } else {
            registrar(nombreHilo + " ERROR: Formato inválido. Use ID,PRODUCTO,CANTIDAD.");
            return;
        }

        // Simulamos el trabajo
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            registrar(nombreHilo + " fue interrumpido");
            return; // Si nos interrumpen (por un shutdownNow), cancelamos
        }

        registrar(nombreHilo + " procesó con éxito el " + pedidoActual.toString());
        
        // Cuidado: Esta operación sin 'synchronized' sufre de condición de carrera
        // (explicado en la semana 4/5 de tu bitácora).
        piezasVendidas = piezasVendidas + 1;
        
        registrar(nombreHilo + " terminó");
    }

    static void registrar(String mensaje) {
        String hora = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
        System.out.println("[" + hora + "] " + mensaje);
    }
}
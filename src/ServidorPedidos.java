import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ServidorPedidos {

    static final String NEGOCIO = "Farmacia SaludYa"; // Nombre de tu negocio

    public static void main(String[] args) throws InterruptedException {

        long pid = ProcessHandle.current().pid();
        registrar("Servidor de " + NEGOCIO + " listo. PID " + pid);

        // TODO resuelto: Imprimir el directorio de arranque
        String directorio = System.getProperty("user.dir");
        registrar("Arrancando en el directorio: " + directorio);

        while (true) {
            Thread.sleep(5000);
            registrar("En espera de pedidos");
        }
    }

    static void registrar(String mensaje) {
        String hora = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println("[" + hora + "] " + mensaje);
    }
}
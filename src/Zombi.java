// Zombi.java - un padre descuidado
public class Zombi {
    public static void main(String[] args) throws Exception {

        ProcessBuilder pb = new ProcessBuilder("sleep", "1");
        Process hijo = pb.start();

        System.out.println("Hijo lanzado con PID " + hijo.pid());

        // TODO: aqui DEBERIA ir hijo.waitFor(), pero lo omitimos a proposito.
        //       El padre se queda vivo un minuto sin recoger al hijo.
        Thread.sleep(60000);
    }
}
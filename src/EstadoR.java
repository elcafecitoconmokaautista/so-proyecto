// EstadoR.java - la version mala de esperar
public class EstadoR {
    public static void main(String[] args) {

        System.out.println("PID " + ProcessHandle.current().pid());

        long inicio = System.currentTimeMillis();

        // Espera exactamente lo mismo que el servidor: 60 segundos.
        // Pero en vez de dormir, se queda preguntando la hora sin parar.
        while (System.currentTimeMillis() - inicio < 60000) {
            // a proposito vacio
        }

        System.out.println("Termine");
    }
}
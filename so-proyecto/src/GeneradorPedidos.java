// GeneradorPedidos.java - imprime N pedidos "ID;PRODUCTO;CANTIDAD" (uso: java GeneradorPedidos 20)
import java.util.Random;

public class GeneradorPedidos {

    // TODO: usa los productos de TU catalogo (los mismos nombres de datos/catalogo.txt)
    static final String[] PRODUCTOS =
        {"paracetamol", "ibuprofeno", "amoxicilina", "loratadina", "omeprazol", "naproxeno"};

    public static void main(String[] args) {
        int n = args.length > 0 ? Integer.parseInt(args[0]) : 10;
        Random azar = new Random();

        System.out.println("Generador arrancado, PID " + ProcessHandle.current().pid());
        for (int i = 1; i <= n; i++) {
            String producto = PRODUCTOS[azar.nextInt(PRODUCTOS.length)];
            int cantidad = 1 + azar.nextInt(3);
            System.out.println(i + ";" + producto + ";" + cantidad);
        }
        System.out.println("Generador termina");
    }
}

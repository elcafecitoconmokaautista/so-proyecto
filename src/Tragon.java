// Tragon.java - pide memoria hasta que alguien lo detenga
import java.util.ArrayList;
import java.util.List;

public class Tragon {
    public static void main(String[] args) throws Exception {

        List<byte[]> bloques = new ArrayList<>();
        int mb = 0;

        while (true) {
            bloques.add(new byte[1024 * 1024]);   // un bloque de 1 MB
            mb++;
            System.out.println("Reservados " + mb + " MB");
            Thread.sleep(50);
        }
    }
}
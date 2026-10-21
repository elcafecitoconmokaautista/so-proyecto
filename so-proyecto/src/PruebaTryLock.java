// PruebaTryLock.java (S7, bloque extra) - evitar el bloqueo con tryLock + tiempo limite.
// Los dos hilos piden los candados en orden cruzado, pero ninguno espera para siempre.
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class PruebaTryLock {

    static final ReentrantLock INVENTARIO = new ReentrantLock();
    static final ReentrantLock LOG = new ReentrantLock();

    static boolean intentarLosDos(ReentrantLock a, ReentrantLock b, String quien) throws InterruptedException {
        if (!a.tryLock(200, TimeUnit.MILLISECONDS)) return false;
        try {
            Thread.sleep(50);                               // da tiempo a que el otro tome su candado
            if (!b.tryLock(200, TimeUnit.MILLISECONDS)) return false;   // el finally suelta 'a'
            try {
                System.out.println(quien + ": tengo los dos");
                return true;
            } finally {
                b.unlock();                                 // con ReentrantLock el unlock es TUYO
            }
        } finally {
            a.unlock();
        }
    }

    static Thread cajero(String nombre, ReentrantLock primero, ReentrantLock segundo) {
        return new Thread(() -> {
            int intentos = 0;
            try {
                while (!intentarLosDos(primero, segundo, nombre)) {
                    intentos++;
                    System.out.println(nombre + ": no pude, reintento " + intentos);
                    Thread.sleep((long) (Math.random() * 100));   // espera aleatoria: evita livelock
                }
            } catch (InterruptedException e) { }
        }, nombre);
    }

    public static void main(String[] args) throws InterruptedException {
        Thread c1 = cajero("cajero-1", INVENTARIO, LOG);
        Thread c2 = cajero("cajero-2", LOG, INVENTARIO);     // orden cruzado a proposito
        c1.start(); c2.start();
        c1.join(); c2.join();
        System.out.println("Los dos terminaron: no hubo interbloqueo.");
    }
}

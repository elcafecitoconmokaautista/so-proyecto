# Guía completa — Sistemas Operativos, semanas 4 a 8

Todo el código de este paquete **compila y fue probado** (hilos, carrera, candado, interbloqueo con `jstack`, señales, tubería, FIFO). Lo que *no* puedo hacer por ti es lo que el curso pide que sea tuyo: **correr los comandos en tu máquina, pegar tus salidas y escribir la bitácora con tus palabras** (en la revisión te preguntan sobre *tu* código y *tus* números). Aquí tienes cómo hacerlo y qué debe decir cada parte.

## 0. Antes de empezar

**Estructura** (cópiala a `~/so-proyecto`, tu repo):

```
src/        Pedido, GeneradorPedidos, GestorInventario (protegido), ServidorPedidos,
            PruebaCarrera, ContadorCarrera, PruebaRendimiento, PruebaBloqueo,
            PruebaTransferencia, PruebaTryLock
versiones/  GestorInventario_roto.java.txt   (la versión SIN candado de la semana 5)
datos/      catalogo.txt
scripts/    compilar.sh y un script por semana (generan las evidencias)
despliegue/ servidor-pedidos.service         (semana 8, extra B)
evidencias/ aquí caen los .txt que pide cada semana
```

**Ajusta a tu dominio** (el curso pide que sea el tuyo): `NEGOCIO` en `ServidorPedidos`, los productos de `GeneradorPedidos`, `datos/catalogo.txt` (`ID;NOMBRE;PRECIO;EXISTENCIA`) y el producto de `PruebaCarrera`. Los nombres del generador y del catálogo deben coincidir.

**Supuesto:** las semanas 1–3 no estaban en los enlaces, así que `Pedido` y `GeneradorPedidos` son reconstrucciones mínimas con el formato `ID;PRODUCTO;CANTIDAD`. Si ya tienes los tuyos, conserva tus clases y deja en `Pedido` los métodos `desdeLinea` y `esLineaDePedido`.

**Rutina:** desde la raíz, `./scripts/compilar.sh` (o `javac src/*.java`) y ejecuta con `java -cp src NombreClase`. Un commit por bloque (`git add . && git commit -m "..." && git pull && git push`). Si te atoras, documéntalo con las 4 partes de *Atorones* y haz commit igual.

---

## Semana 4 — Hilos

**Idea:** un hilo es una línea de ejecución *dentro* del mismo proceso (mismo PID). Comparte montón y archivos abiertos, pero tiene **pila propia**. Crearlo cuesta ~0.1 ms contra ~100 ms de un proceso. Mientras un hilo está bloqueado (disco, red, `sleep`), otro corre: por eso 10 pedidos de 2 s tardan ~2 s y no 20 s.

**Código:** `ServidorPedidos.java` ya hace *un hilo por pedido* (`new Thread(...)` + `start()`, el `while` no espera a nadie) y usa tu `Pedido` (los tres `TODO` resueltos).

**Bloque 1 — qué escribir en `BITACORA.md`**
1. *Recorrido de estados* (mín. 4 transiciones, en tu dominio). Esqueleto: llega el pedido → **Listo** → el planificador lo elige → **Ejecutando** → pide algo (`sleep`/disco) → **Bloqueado** (no compite por CPU) → llega lo esperado → **Listo** → … → se le acaba el quantum → **Listo** (interrupción). Termina el pedido → fin.
2. *Tabla pila/montón:* el objeto inventario → **montón, compartido**. La variable local con el pedido que atiendo → **pila, NO compartida** (cada hilo tiene la suya). El archivo de log abierto → **compartido** (es del proceso).
3. *Predicción* (escríbela **antes** de la grieta): dos hilos leen la misma existencia, los dos descuentan y uno pisa al otro → inventario incorrecto.

**Bloque 2 — evidencia**
```bash
# servidor con 5 pedidos en curso (la entrada se mantiene abierta 30 s para poder medir)
java -cp src ServidorPedidos < <(java -cp src GeneradorPedidos 5; sleep 30) > /tmp/s4.log 2>&1 &
sleep 1; ps -o pid,nlwp,stat,cmd -C java > evidencias/hilos.txt; cat evidencias/hilos.txt
top -H -p <PID>          # ver los hilos pedido-1, pedido-2…   (q para salir)
```
Anota `nlwp` **en reposo** (arranca el servidor sin pedidos y mira `ps`) y **bajo carga**. La diferencia son tus hilos; el resto son hilos internos de la JVM (recolector de basura, compilador JIT…). `ps -ef` muestra una sola línea porque lista **procesos**, y los hilos comparten PID. Pega **tres líneas del log** donde `empieza a atender` de pedidos distintos tengan milisegundos casi iguales (p. ej. `.550`, `.562`, `.570`) y los `termino` salgan juntos 2 s después: eso demuestra simultaneidad.

**Clase — medir secuencial vs. concurrente**
```bash
mkdir -p build_sec
sed -e 's/ServidorPedidos/ServidorSecuencial/g' \
    -e 's/despachar(() -> atender(pedido, numero), "pedido-" + numero);/atender(pedido, numero);/' \
    src/ServidorPedidos.java > build_sec/ServidorSecuencial.java
javac -cp src -d build_sec build_sec/ServidorSecuencial.java
time (java -cp src GeneradorPedidos 10 | java -cp build_sec:src ServidorSecuencial > /dev/null)   # ≈ 20 s
time (java -cp src GeneradorPedidos 10 | java -cp src ServidorPedidos > /dev/null)                # ≈ 2 s
```
En mis pruebas con 5 pedidos: **10.3 s** secuencial vs **2.3 s** con hilos. ¿De dónde sale el tiempo? No se hizo menos trabajo: durante los `sleep` el procesador estaba **ocioso**; los hilos traslapan las esperas. (Si `atender` calculara en vez de esperar, la mejora sería el número de **núcleos**, no de hilos.)
Carga: `java -cp src GeneradorPedidos 200 | java -cp src ServidorPedidos > evidencias/carga200.txt 2>&1 &` y `watch -n 1 'ps -o pid,nlwp,stat,%cpu,%mem -C java'` (anota el máximo de `nlwp` y `%mem`; con 2000 puede salir `OutOfMemoryError: unable to create native thread` — es la evidencia, no la borres).

**La grieta (clase):** `java -cp src ContadorCarrera` suma 1 muchas veces desde muchos hilos y **no da el total**. Escribe antes qué esperas; córrelo **5 veces** y anota los 5 resultados. Si en tu PC sale perfecto (1 núcleo), usa `java -Dventana=true -cp src ContadorCarrera 100 1000`. En el propio servidor también queda el contador `piezasVendidas` deliberadamente sin proteger. **No lo arregles aún**: es el tema de la semana 5.

**Extra (pool):** `java -Dpool=4 -cp src ServidorPedidos` usa `Executors.newFixedThreadPool(4)`: solo 4 pedidos empiezan a la vez y el resto espera en cola. Con 10000 pedidos, un hilo por pedido agota memoria (cada hilo reserva su pila); el pool no. ¿Cuántos hilos? Si tus pedidos *esperan* (disco/red), más que `nproc`; si *calculan*, ≈ `nproc`.

**Bitácora — Avance del proyecto:** qué es un hilo y su diferencia con un proceso (con la tabla de lo que comparten); por qué hilos y no procesos (dos razones medidas: crear cuesta ~1000× menos y **comparten el inventario**); tus 5 resultados del contador y tu hipótesis.
**Integrador:** todos miden `GeneradorPedidos 200 | ServidorPedidos` (tiempo y `nlwp` máximo) en una tabla del README; decidan *hilo por pedido* o *pool de N* y justifíquenlo; anoten qué compartirán los hilos del Servidor Central.

---

## Semana 5 — Condición de carrera

**Idea:** `existencia = existencia - 1` no es atómica: son **LEER, RESTAR, ESCRIBIR**, y el SO puede interrumpir entre cualquier par. Si dos hilos leen el mismo valor, uno pisa al otro (pierdes un descuento). **Sección crítica** = el fragmento que toca el recurso compartido y no puede ejecutarse por dos hilos a la vez; aquí va **desde la consulta (`if`) hasta el descuento**, no solo el `put`. No es determinista, la ventana es de nanosegundos y depende de la máquina, por eso falla en producción y no cuando miras.

**Bloque 1 — bitácora**
1. *Tu tabla de intercalado* con 3 unidades y dos pedidos de 2 en tu dominio. Traza: A lee 3 → B lee 3 → A comprueba `3>=2` ✔ → B comprueba `3>=2` ✔ → A escribe 3−2=**1** → B escribe 3−2=**1** (vendiste 4 y tenías 3). Con varios productos sale **negativo** o incoherente según el intercalado: número imposible = existencia negativa o ventas > existencia.
2. *Sección crítica:* pega `vender()` y justifica: empieza en el `get` porque ahí se **lee el dato con el que se decide** y termina en el `put`/contadores.
3. *Costo:* "dice 3 y no hay ninguno" → vendes lo que no tienes (cobras sin surtir, cliente molesto, reembolsos; en farmacia, un riesgo); "dice 0 y hay 3" → pierdes ventas. No cuestan lo mismo: **sobrevender es peor**.

**Bloque 2 — evidencia**
```bash
./scripts/s05_carrera_rota.sh          # 5 corridas de la versión SIN candado → evidencias/carrera.txt
./scripts/s05_carrera_rota.sh 50 10    # si te cuadran las 5: más cajeros; o 50 3
```
Llena la tabla (corrida / confirmadas / rechazadas / existencia final / ¿cuadra?). Dos síntomas: **(a)** confirmadas > existencias (varios pasaron el `if` con el mismo valor) y **(b)** `inicial − confirmadas ≠ final` (descuentos perdidos). En mis pruebas con 50 cajeros y 10 piezas: **se confirmaron 50 ventas** y la existencia final quedó en 2–4 en cada corrida. Di qué configuración necesitaste y, con números, cuánto habrías cobrado de más en tu peor corrida (ventas confirmadas − piezas reales) × precio.
`cargarDesde` ya está resuelto en `GestorInventario`.

**Extra:** `./scripts/s05_barrido.sh roto` y `./scripts/s05_barrido.sh sinpausa` dan la tasa de fallo por número de cajeros con y sin el `Thread.sleep(1)`. El `sleep` **no crea** el error, solo ensancha la ventana. Con 2 cajeros sin pausa "nunca falla" en tu laptop, **pero no es seguro**: probabilidad baja ≠ cero; con 32 núcleos y 400 cajeros ocurre a diario.

**Clase:** pizarrón de descuadres (más núcleos ⇒ más fallos; nadie obtiene el mismo resultado dos veces ⇒ *una prueba que pasa no demuestra nada*). Quitar el `sleep` no arregla: solo lo vuelve menos probable. Las 3 ideas de arreglo (espera, recomprobar después de descontar, un solo hilo): la 1 funciona pero es lenta; la 2 **parece** funcionar y no funciona (sigue habiendo ventana); la 3 funciona (serializas) pero pierdes el paralelismo. Anota tu apuesta.

**Proyecto:** `ServidorPedidos` ya crea el `GestorInventario`, lo carga de `datos/catalogo.txt`, cada hilo llama a `vender` y registra `CONFIRMADO/RECHAZADO`, y el resumen final imprime recibidos/confirmados/rechazados y, por producto, `inicial`, `vendidas`, `final` y `cuadra`. El curso pide dejar el descuadre **demostrado** esta semana y arreglarlo en la siguiente: la evidencia del sistema roto es la de `PruebaCarrera` con la versión sin candado (`./scripts/s05_carrera_rota.sh`), y la del arreglado es la de la semana 6. El servidor de este paquete ya incluye el arreglo; si tu profesor quiere ver el *servidor* roto en esta entrega, quita temporalmente los `synchronized` de `vender` en tu copia, corre `GeneradorPedidos 200 | ServidorPedidos` con `-Dtrabajo=1` y guarda el resumen (`cuadra=false`) antes de volver a ponerlos.

---

## Semana 6 — Exclusión mutua y semáforos

**Idea:** una solución debe dar **exclusión mutua, progreso y espera acotada**. Una bandera `boolean ocupado` **no funciona** (entre el `while(ocupado)` y `ocupado=true` hay otra ventana): hace falta una operación **atómica** del hardware (`test-and-set`, `compare-and-swap`) envuelta por `synchronized`/`Semaphore`. **Espera activa** (`while(…){}` → estado `R`, ~100 % CPU) vs **espera pasiva** (bloqueo → `S`, ~0 %): el SO no sabe que tu ciclo no hace nada útil; con `synchronized` el hilo se **bloquea** y no gasta CPU. `synchronized` = candado del *objeto* (monitor), se libera solo, incluso con excepción. `Semaphore(N)` = N permisos; con 1 es un *mutex*; el `release()` va **siempre en `finally`**.

**Bloque 1 — bitácora**
1. Traza de la bandera: A y B ven `ocupado==false`, los dos salen del `while`, los dos entran.
2. Respuesta a la semana 2: el ciclo vacío está *listo/ejecutando* y el SO le sigue dando turnos; `sleep`/`synchronized` lo **bloquean** y el SO lo saca de la cola hasta que llegue lo esperado.
3. Tu decisión: `synchronized` en `vender()` (un solo hilo en el inventario). `Semaphore(N)` si tienes un recurso que se ocupa y libera (p. ej. `-Dventanillas=3`: tres ventanillas, el cuarto espera **bloqueado**).

**Bloque 2 — evidencia**
`src/GestorInventario.java` ya está protegido: `vender` es `synchronized` con **consulta y descuento bajo el mismo candado**; los lectores y contadores también (si los dejas fuera, se pierden). Además conserva `versiones/GestorInventario_roto.java.txt`.
```bash
./scripts/s06_carrera_resuelta.sh 50 10   # evidencias/carrera_resuelta.txt + diff con la versión rota
```
En mis pruebas con 50 cajeros y 10 piezas: **10 confirmadas, 0 final y `CUADRA = true` en las cinco corridas**, siempre idéntico. Por qué: dejó de poder ocurrir que dos hilos estén entre la consulta y el descuento a la vez; el resultado ya no depende del intercalado. `cargarDesde` no necesita `synchronized` propio porque se llama al arrancar con un solo hilo y cada alta pasa por `agregar()`, que sí está protegido. Servidor completo: `java -cp src GeneradorPedidos 200 | java -cp src ServidorPedidos > evidencias/servidor_protegido.txt 2>&1` (cada línea `cuadra=true`).

**Extra:** `./scripts/s06_rendimiento.sh` (sin `sleep`): con 1 hilo casi no hay diferencia; con más, la versión protegida **no escala** (atiende de uno en uno) y la rota sube pero da resultados incorrectos. Propuesta de diseño: **un candado por producto** (granularidad), que trae el interbloqueo de la semana 7.

**Clase (rompe tu solución):** (a) candado **solo en el descuento** → vuelve a fallar, existencia **negativa**: regla = *la sección crítica empieza donde se lee el dato con el que se decide*. (b) `synchronized` en todo `atender` → correcto pero ≈ **20 s en vez de 2**: regla = *lo más chica posible, pero no más chica que lo que debe ocurrir junto*. (c) `Semaphore(3)`: entran de 3 en 3 y el resto está bloqueado (`%CPU ≈ 0`).

---

## Semana 7 — Bloqueos (interbloqueo)

**Idea:** hilos que esperan, cada uno, un recurso que tiene otro; nadie avanza. Y **no hay síntoma**: `ps` lo ve como un proceso sano (`S`, 0 % CPU), igual que uno ocioso. **Coffman — las 4 condiciones simultáneas:** exclusión mutua, retención y espera, sin expropiación, espera circular. Romper una basta. En la práctica se rompe la **espera circular**: numerar los recursos y pedirlos **siempre en el mismo orden**. Estrategias: prevenir (orden global), evitar (algoritmo del banquero, casi no se usa) y detectar/recuperar (las bases de datos matan una "víctima"). Linux no detecta interbloqueos entre tus hilos.

**Bloque 1 — bitácora**
1. Tu escenario (dos hilos, dos recursos en orden cruzado): A toma INVENTARIO y pide LOG; B toma LOG y pide INVENTARIO.
2. Tabla de Coffman: exclusión mutua ✔ (`synchronized`), retención y espera ✔ (cada uno conserva el suyo mientras pide el otro), sin expropiación ✔ (el SO no puede quitarle un monitor), espera circular ✔ (A→B→A).
3. Orden global (lista numerada y fija), p. ej.: **1 INVENTARIO, 2 LOG, 3 contadores, 4 archivo de recibos**. Regla: quien necesite varios los pide en orden creciente.
*Tu servidor real:* solo toma **un** candado (el del inventario; `registrar` no toma ninguno propio), así que no tiene el problema. Dilo explícitamente en la bitácora: no tener el problema también es un resultado, si lo comprobaste.

**Bloque 2 — evidencia**
```bash
./scripts/s07_deadlock.sh      # lo provoca, hace ps y jstack → evidencias/deadlock.txt, y lo arregla
# a mano: java -cp src PruebaBloqueo &   → jstack <PID> > evidencias/deadlock.txt → kill -9 <PID>
```
En `jstack` busca `Found one Java-level deadlock` y, en cada hilo, `locked <0x…>` (lo que **tiene**) y `waiting to lock <0x…>` (lo que **espera**): cruza los números y verás el ciclo cajero-1 ⇄ cajero-2. Pega las 4 líneas y explícalas. Pega también tu `ps` (estado `S`, ~0 % CPU: indistinguible de un servidor sano). Arreglo: `java -cp src PruebaBloqueo ordenado` (ambos piden INVENTARIO y luego LOG) → rompiste la **espera circular**.
*Si no tienes `jstack`:* viene con el JDK completo (`sudo apt install default-jdk-headless` en Ubuntu/WSL).

**Extra:** `java -cp src PruebaTryLock` (con `ReentrantLock.tryLock` + tiempo límite + espera aleatoria). El `finally` con `unlock()` es obligatorio; sin espera aleatoria puede haber **livelock** (trabajan sin avanzar y gastan CPU). Tabla: orden global *previene*, costo casi nulo, no sirve con código de terceros; `tryLock` *evita* con reintentos, cuesta algo, funciona con terceros, introduce livelock.

**Clase:** `java -cp src PruebaTransferencia` (un candado por producto, transferencias en sentidos opuestos: **el orden lo ponen los datos**, es el caso del banco) se traba; `java -cp src PruebaTransferencia ordenado` lo resuelve ordenando por `compareTo` del nombre. `kill` normal no mata al proceso trabado (SIGTERM necesita ejecutar un manejador y todos sus hilos están bloqueados); `kill -9` sí → semana 8.

---

## Semana 8 — Comunicación entre procesos

**Idea:** los procesos **no comparten memoria**; el kernel presta canales: **señales** (solo un número, un aviso), **tuberías** (flujo de bytes en un sentido, 64 KB, bloquea al escritor si se llena, solo entre emparentados), **FIFO** (tubería con nombre, un archivo `p`; sirve entre procesos no emparentados pero **no entre máquinas**), **sockets** (entre máquinas, semana 16). `SIGINT`(2)=Ctrl+C, `SIGTERM`(15)=`kill`, `SIGKILL`(9) y `SIGSTOP`(19) **no se atrapan**, `SIGHUP`(1)=terminal cerrada, `SIGCHLD`=terminó un hijo (el zombi). `SIGKILL` la ejecuta el **kernel** sin avisar al proceso: es la garantía de último recurso, y por eso **no se ejecuta tu limpieza**.

**Bloque 1 — bitácora**
1. Tu proceso trabado no atendió `kill` (SIGTERM) porque para ejecutar el manejador tiene que poder ejecutar algo y todos sus hilos estaban bloqueados; `kill -9` no le pide nada al proceso.
2. Plan de apagado (en orden): dejar de aceptar pedidos → terminar los que están en curso → escribir pendientes/cerrar archivos → imprimir resumen → salir. Qué queda a medias en tu negocio si muere en seco: ventas cobradas sin descontar, recibos sin escribir, el inventario sin guardar.
3. Tabla: avisar que se apague → **señal** (`SIGTERM`); generador → servidor → **tubería** (`|` o `ProcessBuilder`); cajero en otra máquina → **socket** (ni la tubería ni el FIFO cruzan máquinas).

**Bloque 2 — evidencia**
`ServidorPedidos` ya trae el *shutdown hook* (con `volatile boolean apagando`, espera a los pedidos con tope de 10 s, resumen final) y la tubería desde Java (`--generador N`, lee `hijo.getInputStream()`; `esLineaDePedido` descarta "arrancado/termina").
```bash
./scripts/s08_senales.sh          # SIGTERM, SIGINT y SIGKILL → evidencias/senales.txt
java -cp src ServidorPedidos --generador 20     # tubería con el hijo
```
Resultado (mis pruebas, 20 pedidos): `SIGTERM` y `SIGINT` → muestran el mensaje de apagado, **los 20 pedidos terminan** y sale el resumen; `SIGKILL` → **0 terminados y sin resumen**, el log se corta a media línea. Pega las tres salidas y señala dónde se corta la de `SIGKILL`; di qué venta/recibo/línea de inventario se perdió. `volatile` garantiza que los hilos **vean** el cambio de la bandera (sin él pueden seguir con una copia en caché); **no** hace atómico `contador++`.
*Nota:* lanzar el servidor con `&` sin tubería le da entrada vacía y termina solo; por eso el script usa `< <(…; sleep 300)`.

**Extra A (FIFO):** `mkfifo datos/cajero.pipe`; `java -cp src ServidorPedidos --fifo datos/cajero.pipe`; desde otra terminal `echo "1;paracetamol;2" > datos/cajero.pipe` o `java -cp src GeneradorPedidos 50 > datos/cajero.pipe`. Abrir un FIFO bloquea hasta que haya lector y escritor; cuando el escritor cierra llega fin de archivo (el servidor lo reabre para el siguiente cajero).
**Extra B (systemd):** activa systemd en WSL (`/etc/wsl.conf` → `[boot] systemd=true`, luego `wsl --shutdown` desde PowerShell); copia `despliegue/servidor-pedidos.service` a `/etc/systemd/system/` (cambia `TU_USUARIO`); `sudo systemctl daemon-reload && sudo systemctl start servidor-pedidos`. El servicio usa `--fifo` porque systemd le da entrada vacía. `stop` manda SIGTERM, espera `TimeoutStopSec` y después SIGKILL; un hook de 10 min lo fuerza (míralo con `journalctl -u servidor-pedidos`).

**Clase — cuántos pedidos se pierden:**
```bash
java -cp src GeneradorPedidos 200 | java -cp src ServidorPedidos > /tmp/term.log 2>&1 &
sleep 3; kill -15 $(pgrep -f "[S]ervidorPedidos"); grep -c "termino" /tmp/term.log
# repite con kill -9 y compara; también SIGSTOP (-19) y SIGCONT (-18): el proceso se congela en estado T y sigue donde iba
```
**Cadena completa (cierre de la Unidad 2):** `java -cp src GeneradorPedidos 500 | java -cp src ServidorPedidos > evidencias/unidad2.txt 2>&1 &` → `jstack` (hilos vivos) → `kill -15` → `tail -10 evidencias/unidad2.txt` (el resumen cuadra).

**Prepara la revisión (semana 9):** `BITACORA.md` completa de la semana 1 a la 8 (30 %), `push` **antes** de la fecha, apagado ordenado funcionando en vivo, y en el integrador: mismo orden de candados y mismo criterio de apagado en todas las sucursales, autoevaluación entre pares, y un ensayo cronometrado de la demo (carrera → arreglado → interbloqueo con `jstack` → apagado).

---

## Respuestas cortas a las preguntas de revisión más difíciles

- *Hilos vs procesos:* los hilos comparten espacio de direcciones (montón, archivos) y tienen pila propia; crear uno es ~1000× más barato.
- *10 pedidos de 2 s en ~2 s:* el CPU estaba ocioso durante los `sleep`; los hilos traslapan esperas. Con cálculo puro la mejora sería el número de núcleos.
- *10000 hilos matan al servidor:* se acaba la **memoria** de las pilas (como los PID con los zombis).
- *Una corrida correcta de cinco:* no demuestra nada; la prueba que pasa no prueba ausencia de condición de carrera.
- *Quité el `sleep` y dejó de fallar:* no se arregló; solo se estrechó la ventana.
- *N hilos descontando a la vez:* se pueden perder hasta **N−1** descuentos.
- *¿`while(ocupado){}` vs `synchronized`?* El primero queda listo/ejecutando (100 % CPU); el segundo se bloquea (0 %).
- *Candado por producto → ¿qué problema trae?* Interbloqueo (el orden lo ponen los datos); se resuelve ordenando por una propiedad total (`compareTo`).
- *`tryLock` con reintento:* puede causar livelock; se evita con espera aleatoria.
- *Servidor colgado 3 h, `ps` normal, sin log:* `jstack`, buscar `deadlock`, cruzar `locked`/`waiting to lock`.
- *¿`volatile` hace seguro `contador++`?* No: da visibilidad, no atomicidad.
- *Hook de 3 min con systemd:* lo mata con SIGKILL al vencer `TimeoutStopSec` y se pierde la limpieza.

# BITACORA.md — plantilla semanas 4 a 8
(Escribe con tus palabras y pega TUS salidas. Las ideas clave de cada punto están en GUIA.md.)

## Semana 4 — Hilos
### Antes de la clase
- [ ] Recorrido de estados de mi servidor (mín. 4 transiciones y qué provoca cada una)
- [ ] Tabla pila/montón (inventario · variable local del pedido · archivo de log)
- [ ] Predicción: qué puede salir mal con dos hilos descontando del mismo inventario
- [ ] Hilos en reposo: ___ · bajo carga: ___ · de dónde salen los que no creé
- [ ] 3 líneas del log que muestran pedidos traslapados (señalando los milisegundos)
- [ ] Por qué `ps -ef` muestra una sola línea
### Durante la clase
| | Tiempo real | Cada pedido | 10 pedidos deberían tardar… |
|---|---|---|---|
| ServidorSecuencial | | 2 s | |
| ServidorPedidos (hilos) | | 2 s | |
- [ ] De dónde salió el tiempo · máximo `nlwp` con 200 pedidos: ___ (`%mem` ___) · 5 resultados del contador: ___ ___ ___ ___ ___
### Avance del proyecto
- [ ] Qué es un hilo vs un proceso · por qué hilos (2 razones medidas) · mi hipótesis del contador
- [ ] (Extra) pool vs hilo por pedido: nlwp · simultáneos · tiempo del último · qué pasa con 10000

## Semana 5 — Condición de carrera
### Antes de la clase
- [ ] Mi tabla de intercalado (3 unidades, dos pedidos de 2) y el número imposible
- [ ] Mi sección crítica (código) y por qué empieza/termina ahí
- [ ] Costo en mi negocio: "dice 3 y hay 0" vs "dice 0 y hay 3"
| Corrida | Confirmadas | Rechazadas | Existencia final | ¿Cuadra? |
|---|---|---|---|---|
| 1 | | | | |
| 2 | | | | |
| 3 | | | | |
| 4 | | | | |
| 5 | | | | |
- [ ] Cuál de los 2 síntomas me salió · qué configuración necesité · cuánto habría cobrado de más/menos
- [ ] (Extra) tabla de tasa de fallo con y sin `sleep`; ¿2 cajeros sin fallo = seguro? ¿por qué no?
### Durante la clase
- [ ] Las 3 soluciones del pizarrón y mi apuesta de cuál funciona / cuál es cara / cuál solo parece funcionar
### Avance del proyecto
- [ ] Qué es una condición de carrera · mi sección crítica con código · por qué este error es distinto a los demás · apuesta

## Semana 6 — Exclusión mutua y semáforos
### Antes de la clase
- [ ] Por qué la bandera `boolean ocupado` falla (tabla de intercalado)
- [ ] Respuesta a la semana 2: ciclo vacío 100 % CPU vs bloqueo 0 %, y su relación con `synchronized`
- [ ] Mi mecanismo y por qué · dónde me conviene `Semaphore(N)` y cuánto vale N
| Corrida | Antes: confirmadas | Antes: final | Después: confirmadas | Después: final |
|---|---|---|---|---|
| 1 | | | | |
| 2 | | | | |
| 3 | | | | |
| 4 | | | | |
| 5 | | | | |
- [ ] Dónde empieza/termina mi sección crítica y qué pasa si protejo solo el descuento
- [ ] Por qué ahora las 5 corridas dan lo mismo · si `cargarDesde` necesita `synchronized` y por qué
- [ ] (Extra) tabla de rendimiento 1/2/4/8/16/32 hilos y mi propuesta de candado por producto
### Durante la clase
- [ ] Regla del candado demasiado chico · regla del candado demasiado grande
### Avance del proyecto
- [ ] Exclusión mutua + 3 propiedades · mecanismo elegido y dónde · antes/después · las dos reglas

## Semana 7 — Bloqueos
### Antes de la clase
- [ ] Mi escenario de interbloqueo (dos hilos, dos recursos de mi sistema)
| Condición | ¿Se cumple? ¿Por qué? |
|---|---|
| Exclusión mutua | |
| Retención y espera | |
| Sin expropiación | |
| Espera circular | |
- [ ] Mi orden global de recursos (lista numerada y la regla)
- [ ] Las 4 líneas de `jstack` que prueban el ciclo, explicadas
- [ ] Mi `ps` del proceso trabado y por qué se ve sano
- [ ] Qué condición rompí al arreglarlo y por qué esa
- [ ] (Extra) comparación orden global vs `tryLock`; qué es un livelock
### Durante la clase
- [ ] Mi diagnóstico a ciegas: hilos en el ciclo · candado de cada uno · línea donde está trabado
### Avance del proyecto
- [ ] Qué es un interbloqueo y las 4 condiciones · cómo se diagnostica (comandos) · qué condición rompo · `ps` trabado vs ocioso

## Semana 8 — Comunicación entre procesos
### Antes de la clase
- [ ] Por qué `SIGKILL` no se atrapa y qué le pasó a mi proceso trabado con `kill`
- [ ] Mi plan de apagado ordenado (mín. 3 pasos, de mi dominio)
| Necesito… | Mecanismo | Por qué |
|---|---|---|
| Avisarle a mi servidor que se apague | | |
| Mandarle pedidos desde el generador | | |
| Un cajero en otra máquina | | |
- [ ] Las tres salidas comparadas (SIGTERM / SIGINT / SIGKILL) y dónde se corta el log de SIGKILL
- [ ] Qué se perdió con SIGKILL en mi negocio · por qué la bandera es `volatile`
- [ ] (Extra A o B) tabla FIFO/tubería/socket, o `systemctl status` + pruebas de `journalctl`
### Durante la clase
| | Recibidos | Terminados | Perdidos | ¿Resumen final? |
|---|---|---|---|---|
| SIGTERM | | | | |
| SIGKILL | | | | |
### Avance del proyecto
- [ ] Qué es una señal · qué hace mi servidor al apagarse y por qué ese orden · cómo se comunican procesos sin memoria compartida · pedidos perdidos con cada señal

| PID | PPID | STAT | Qué creo que es y por qué está en ese estado |
|:---:|:----:|:----:|----------------------------------------------|
| 250 | 1 | Ss | Es una instancia secundaria de init. Está en estado durmiente (S) porque esta espenado iniciar algo|
| 251 | 250 | S | Es la shell de bash del usuario. y esta durmiendo (S) esperando la entrada del usuario (teclado/comandos en la terminal). |
| 4 | 1 | Sl | Es el servicio de sistema de archivos plano. Está durmiente (S) esperando alguna peticion y corre en varios hilos |

### 3. Dos preguntas cortas sobre la raíz del árbol

* **El proceso con PID 1, ¿quién es y qué PPID tiene?**
  El proceso con `PID 1` es el proceso de inicialización del sistema (generalmente `systemd` o `init` en Linux). Al buscarlo con el comando `ps -ef | head -3`, notarás que **su PPID es 0**. El número 0 significa que este proceso no tiene un proceso padre en el espacio de usuario, sino que fue invocado directamente por el propio kernel del sistema operativo.

* **¿Por qué ese proceso es el único que no pudo ser creado por otro proceso?**
  Esto se debe a la cadena de arranque del sistema operativo. Una vez que el kernel (núcleo) se carga en la memoria e inicializa el hardware esencial, necesita levantar el "espacio de usuario". Para lograrlo, el kernel ejecuta de forma manual y directa el primer programa (`init`/`systemd`), asignándole el primer identificador disponible (`PID 1`). Al ser literalmente el primer proceso, es físicamente imposible que haya sido creado por otro proceso previo.

---

### Análisis de Procesos (Continuación de tabla)

| PID | PPID | STAT | Qué creo que es y por qué está en ese estado |
|:---:|:----:|:----:|:---------------------------------------------|
| 1   | 0    | Ss   | Es el proceso `init` (o `systemd`), la raíz de todos los procesos. Su PPID es 0 porque fue creado directamente por el kernel. Está en estado durmiente (S) porque se mantiene en segundo plano esperando señales para administrar los servicios del sistema. |
| 250 | 1    | Ss   | Es una instancia secundaria de init. Está en estado durmiente (S) porque está esperando iniciar algo. |
| 251 | 250  | S    | Es la shell de bash del usuario y está durmiendo (S) esperando la entrada del usuario (teclado/comandos en la terminal). |
| 4   | 1    | S1   | Es el servicio de sistema de archivos plano. Está durmiente (S) esperando alguna petición y corre en varios hilos. |

### Antes de la clase:

*   **Tu PID y tu PPID, y quién es el padre de tu servidor:**
    Al buscar el proceso `ServidorPedidos` con el comando `ps`, encontré que su **PID es 2195** y su **PPID es 335**. El padre de mi servidor (el proceso 335) es la terminal o sesión de shell (bash en la pseudo-terminal `pts/0`) desde la cual arranqué el programa en Java.

*   **En qué estado lo encontraste y por qué en ese y no en otro:**
    El proceso se encuentra en estado **Sl**. 
    La **"S"** (Interruptible Sleep) significa que el proceso está "durmiendo" a la espera de un evento; en el caso de un servidor, está esperando que llegue una petición de un cliente por la red. La **"l"** minúscula (is multi-threaded) indica que el proceso está corriendo en múltiples hilos, un comportamiento por defecto de la Máquina Virtual de Java (JVM). Está en este estado y no en "R" (Running) porque, al no haber peticiones entrantes en ese momento exacto, cede el uso del procesador en lugar de consumirlo innecesariamente.

*   **Qué %cpu tenía, y por qué ese número es el correcto para un servidor que "no está haciendo nada":**
    El consumo de procesador era del **0.4%**. Este número es totalmente correcto y esperado para un servidor inactivo (idle). Un servidor que "no hace nada" está en estado de reposo (S) y no requiere ciclos intensivos del procesador. El pequeño porcentaje de 0.4% se debe únicamente a tareas menores de mantenimiento en segundo plano que realiza Java (como el recolector de basura o mantener abierto el puerto de escucha), lo que demuestra que el proceso está bien optimizado y no está bloqueando el sistema.
| PID | PPID | STAT | Qué creo que es y por qué está en ese estado |
|:---:|:----:|:----:|----------------------------------------------|
| 250 | 1 | Ss | Es una instancia secundaria de init. Está en estado durmiente (S) porque esta espenado iniciar algo|
| 251 | 250 | S | Es la shell de bash del usuario. y esta durmiendo (S) esperando la entrada del usuario (teclado/comandos en la terminal). |
| 4 | 1 | Sl | Es el servicio de sistema de archivos plan9. Está durmiente (S) esperando alguna peticion y corre en varios hilos |
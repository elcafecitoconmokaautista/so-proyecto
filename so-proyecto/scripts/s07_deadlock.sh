#!/bin/bash
# S7 - provoca el interbloqueo, lo diagnostica con jstack y guarda evidencias/deadlock.txt.
cd "$(dirname "$0")/.." || exit 1
javac src/*.java || exit 1
mkdir -p evidencias
java -cp src PruebaBloqueo > /tmp/bloqueo.log 2>&1 &
PID=$!
sleep 2
echo "--- salida del programa (se queda a medias) ---"; cat /tmp/bloqueo.log
echo "--- ps: se ve SANO (estado S, ~0% CPU) ---"
ps -o pid,nlwp,stat,%cpu,%mem,etime,cmd -p $PID | tee evidencias/ps_bloqueado.txt
echo "--- jstack ---"
jstack $PID > evidencias/deadlock.txt 2>&1
grep -A 25 "Found one Java-level deadlock" evidencias/deadlock.txt
kill -9 $PID
echo "--- arreglado: mismo orden para todos ---"
java -cp src PruebaBloqueo ordenado

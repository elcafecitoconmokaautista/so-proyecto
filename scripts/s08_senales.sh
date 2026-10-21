#!/bin/bash
# S8 - manda SIGTERM, SIGINT y SIGKILL al servidor con pedidos en curso y guarda evidencias/senales.txt.
# (La entrada del servidor es "generador; sleep 300" para que NO vea fin de archivo y termine solo.)
cd "$(dirname "$0")/.." || exit 1
javac src/*.java || exit 1
mkdir -p evidencias
probar() {   # $1 = nombre, $2 = señal
  java -cp src ServidorPedidos < <(java -cp src GeneradorPedidos 20; sleep 300) > /tmp/s_$1.log 2>&1 &
  PID=$!
  sleep 2
  kill -$2 $PID
  sleep 4
  pkill -f "sleep 300" 2>/dev/null
}
probar sigterm 15
probar sigint 2
probar sigkill 9
{ echo "=== SIGTERM (15) ==="; cat /tmp/s_sigterm.log
  echo "=== SIGINT (2) ===";   cat /tmp/s_sigint.log
  echo "=== SIGKILL (9) ===";  cat /tmp/s_sigkill.log; } > evidencias/senales.txt
for s in sigterm sigint sigkill; do
  echo "$s -> hay resumen final? $(grep -c 'Resumen final' /tmp/s_$s.log)   pedidos terminados: $(grep -c ' termino' /tmp/s_$s.log)"
done

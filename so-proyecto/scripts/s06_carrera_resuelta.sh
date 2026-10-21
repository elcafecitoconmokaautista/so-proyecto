#!/bin/bash
# S6 - la MISMA prueba con el inventario protegido (src/GestorInventario.java), 5 corridas + diff.
cd "$(dirname "$0")/.." || exit 1
CAJ=${1:-10}; EXI=${2:-10}
javac src/*.java || exit 1
mkdir -p evidencias
for i in 1 2 3 4 5; do
  echo "===== CORRIDA $i ====="
  java -cp src PruebaCarrera "$CAJ" "$EXI"
done > evidencias/carrera_resuelta.txt 2>&1
grep -E "CORRIDA|confirmadas|final|CUADRA" evidencias/carrera_resuelta.txt
echo "--- diff contra la version rota (si existe evidencias/carrera.txt) ---"
[ -f evidencias/carrera.txt ] && diff evidencias/carrera.txt evidencias/carrera_resuelta.txt | head -40

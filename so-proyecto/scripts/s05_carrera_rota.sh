#!/bin/bash
# S5 - compila la version ROTA del inventario aparte y corre 5 veces PruebaCarrera.
# Uso: ./scripts/s05_carrera_rota.sh [cajeros] [existencia]     (por omision 10 10; prueba 50 10 o 50 3)
cd "$(dirname "$0")/.." || exit 1
CAJ=${1:-10}; EXI=${2:-10}
rm -rf build_roto && mkdir build_roto
cp versiones/GestorInventario_roto.java.txt build_roto/GestorInventario.java
javac -d build_roto build_roto/GestorInventario.java src/PruebaCarrera.java || exit 1
mkdir -p evidencias
for i in 1 2 3 4 5; do
  echo "===== CORRIDA $i ====="
  java -cp build_roto PruebaCarrera "$CAJ" "$EXI"
done > evidencias/carrera.txt 2>&1
grep -E "CORRIDA|confirmadas|final|CUADRA" evidencias/carrera.txt
echo "(salida completa en evidencias/carrera.txt)"

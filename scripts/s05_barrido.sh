#!/bin/bash
# S5 extra - tasa de fallo segun numero de cajeros. Uso: ./scripts/s05_barrido.sh [roto|sinpausa]
# "roto" = version rota con pausa de 1 ms.  "sinpausa" = version rota SIN la pausa (-Dpausa=false).
cd "$(dirname "$0")/.." || exit 1
MODO=${1:-roto}; OPC=""; [ "$MODO" = "sinpausa" ] && OPC="-Dpausa=false"
rm -rf build_roto && mkdir build_roto
cp versiones/GestorInventario_roto.java.txt build_roto/GestorInventario.java
javac -d build_roto build_roto/GestorInventario.java src/PruebaCarrera.java || exit 1
for cajeros in 2 5 10 50 100 500; do
  fallos=0
  for i in $(seq 1 20); do
    java $OPC -cp build_roto PruebaCarrera $cajeros 10 | grep -q "CUADRA.*false" && fallos=$((fallos+1))
  done
  echo "cajeros=$cajeros  fallos en 20 corridas: $fallos"
done

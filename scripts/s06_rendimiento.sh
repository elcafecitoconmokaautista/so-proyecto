#!/bin/bash
# S6 extra - throughput sin candado (rota) vs con candado (protegida), SIN la pausa de 1 ms.
cd "$(dirname "$0")/.." || exit 1
javac src/*.java || exit 1
rm -rf build_roto && mkdir build_roto
cp versiones/GestorInventario_roto.java.txt build_roto/GestorInventario.java
javac -d build_roto build_roto/GestorInventario.java src/PruebaRendimiento.java || exit 1
echo "### SIN synchronized (rota: rapida y MAL)";   for h in 1 2 4 8 16 32; do java -Dpausa=false -cp build_roto PruebaRendimiento $h; done
echo "### CON synchronized (protegida)";            for h in 1 2 4 8 16 32; do java -Dpausa=false -cp src PruebaRendimiento $h; done

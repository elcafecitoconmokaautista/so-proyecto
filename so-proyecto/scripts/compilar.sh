#!/bin/bash
# Compila todo el proyecto (corre desde la raiz: ./scripts/compilar.sh)
cd "$(dirname "$0")/.." || exit 1
javac src/*.java && echo "Compilado en src/"

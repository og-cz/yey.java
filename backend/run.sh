#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

find src -name "*.java" > sources.txt
mkdir -p out

echo "Compiling..."
javac -d out @sources.txt

echo "Starting server on http://localhost:8080 ..."
java -cp out com.library.Main

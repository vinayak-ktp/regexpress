#!/usr/bin/env bash
# Compile every .java file under src/main/java into out/production, then run Main.
set -euo pipefail
cd "$(dirname "$0")"

find src/main/java -name '*.java' > out.sources
javac -d out/production @out.sources
rm out.sources

java -cp out/production com.regexpress.Main

#!/usr/bin/env bash
# Compile every .java file under src/main/java into out/production, then run Main.
set -euo pipefail
cd "$(dirname "$0")"

# Clear out/production first, so a renamed or deleted class never leaves
# stale bytecode behind that no longer matches the current source.
rm -rf out/production
mkdir -p out/production

find src/main/java -name '*.java' > out.sources
javac -d out/production @out.sources
rm out.sources

java -cp out/production com.regexpress.Main

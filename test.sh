#!/usr/bin/env bash

set -euo pipefail
cd "$(dirname "$0")"

rm -rf out/production
mkdir -p out/production

find src/main/java src/test/java -name '*.java' > out.sources
javac -d out/production @out.sources
rm out.sources

# pass package.FileName (example: ast.AstTest)
java -cp out/production com.regexpress.$1

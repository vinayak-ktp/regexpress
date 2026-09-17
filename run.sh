#!/usr/bin/env bash
# Compile everything under src/main/java and src/test/java, then run one class.
#
# Usage:
#   ./run.sh                     -> compiles, then runs com.regexpress.Main
#   ./run.sh ast.AstTest         -> compiles, then runs com.regexpress.ast.AstTest
#   ./run.sh com.regexpress.ast.AstTest   -> also works: full names pass through as-is
set -euo pipefail
cd "$(dirname "$0")"

# Read Java version from .java-version file
if [ -f .java-version ]; then
    JAVA_VERSION=$(cat .java-version)
    JAVA_HOME=$(/usr/libexec/java_home -v "$JAVA_VERSION")
fi

# Clear out/production first, so a renamed or deleted class never leaves
# stale bytecode behind that no longer matches the current source.
rm -rf out/production
mkdir -p out/production

find src/main/java src/test/java -name '*.java' > out.sources
"${JAVA_HOME}/bin/javac" -d out/production @out.sources
rm out.sources

# ${1:-Main} means: use the first argument if one was given, otherwise
# fall back to "Main". This makes the argument optional rather than required.
target="${1:-Main}"

# If what was passed already looks fully-qualified (contains a dot before
# the class name, e.g. "com.regexpress.ast.AstTest"), use it as-is.
# Otherwise, assume it's shorthand relative to com.regexpress
# (e.g. "ast.AstTest" or just "Main") and prepend the package.
if [[ "$target" == com.regexpress.* ]]; then
    className="$target"
else
    className="com.regexpress.$target"
fi

"${JAVA_HOME}/bin/java" -cp out/production "$className"

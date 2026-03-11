#!/usr/bin/env bash
set -euo pipefail

# Avoid Snap-injected GTK/locale modules breaking the JVM linker.
unset GTK_PATH
unset GTK_EXE_PREFIX
unset GIO_MODULE_DIR
unset GSETTINGS_SCHEMA_DIR
unset GTK_IM_MODULE_FILE
unset LOCPATH
unset LD_PRELOAD

cd "$(dirname "$0")"
exec mvn clean compile exec:java

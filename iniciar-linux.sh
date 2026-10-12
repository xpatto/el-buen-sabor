#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")"

java_demo=java
if [[ -n "${JAVA_HOME:-}" ]]; then
    java_demo="$JAVA_HOME/bin/java"
fi
if ! command -v "$java_demo" >/dev/null 2>&1; then
    printf '%s\n' 'No se encontro Java. Instala JDK 21 y revisa PATH o JAVA_HOME.' >&2
    exit 1
fi
if ! "$java_demo" scripts/VerificarDemo.java; then
    printf '%s\n' 'No se pudo iniciar. Se requiere JDK 21 y el puerto 8080 libre.' >&2
    exit 1
fi

printf '%s\n' \
    'El Buen Sabor - demostracion LOCAL (solo 127.0.0.1).' \
    'La primera ejecucion puede tardar: Maven Wrapper descarga Maven y dependencias.' \
    'Espera el mensaje Started MenuApplication antes de abrir:' \
    'Menu: http://localhost:8080/' \
    'Administracion: http://localhost:8080/admin (usuario: admin, clave: admin)' \
    'Base independiente: data/menu-demo.db. Los cambios se conservan al reiniciar.' \
    'Detener: Ctrl+C en esta terminal.'

if sh ./mvnw spring-boot:run -Dspring-boot.run.profiles=demo \
    '-Dspring-boot.run.arguments=--spring.profiles.active=demo --server.address=127.0.0.1 --server.port=8080 --spring.datasource.url=jdbc:sqlite:./data/menu-demo.db'; then
    exit 0
else
    result=$?
    printf '%s\n' 'El servidor se detuvo. Si no fue por Ctrl+C, revisa el error anterior, JDK 21, la conexion inicial y el puerto 8080.' >&2
    exit "$result"
fi

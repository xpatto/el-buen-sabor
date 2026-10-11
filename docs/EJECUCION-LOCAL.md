# Ejecución local y cierre de entrega

El objetivo es clonar y ejecutar en el equipo del lector. No se requiere
alojamiento, dominio, Docker, despliegue ni configuración de producción.
Las instrucciones principales para Fish están al principio del README.

## Bash en Linux

Necesitas Git y JDK 21 en PATH. Ejecuta en una terminal interactiva, sin `set -x`:

```bash
git clone https://github.com/xpatto/el-buen-sabor.git
cd el-buen-sabor
java -version
javac -version
./mvnw -q dependency:build-classpath -DincludeScope=runtime -Dmdep.outputFile=target/local-classpath.txt
config_local=$(mktemp -d)
java --class-path "$(cat target/local-classpath.txt)" scripts/GenerarHash.java "$config_local/password.bcrypt"
```

Verifica Java 21 y continúa solo si Maven y el generador terminaron correctamente.
El generador pide contraseña y confirmación sin eco. No pases la contraseña como
argumento; no compartas el archivo BCrypt, que también es sensible.

```bash
read -r -p 'Usuario administrativo local: ' ADMIN_USERNAME
export ADMIN_USERNAME
export ADMIN_PASSWORD_HASH="$(cat "$config_local/password.bcrypt")"
rm -- "$config_local/password.bcrypt"
rmdir -- "$config_local"
export SPRING_PROFILES_ACTIVE=dev
./mvnw spring-boot:run
```

Abre http://localhost:8080/ y http://localhost:8080/admin. En el login usa la
contraseña original. Detén con Ctrl+C y ejecuta:

```bash
unset ADMIN_USERNAME ADMIN_PASSWORD_HASH SPRING_PROFILES_ACTIVE
```

Para un catálogo de prueba nuevo, con las credenciales configuradas y el
servidor anterior detenido:

```bash
prueba_local=$(mktemp -d)
APP_DB_PATH="$prueba_local/menu.db" ./mvnw spring-boot:run
```

No elimina ni reemplaza data/menu.db. Las pruebas automáticas son los mismos
comandos Maven/Node del README y no necesitan credenciales reales.

## Windows con PowerShell

Alternativa documentada; **no ejecutada en Windows durante este cierre**.
Instala Git y JDK 21, abre una consola PowerShell interactiva (no ISE), y comprueba
que java y javac resuelven el JDK correcto. No actives transcripción de terminal
ni guardes credenciales con `setx`.

```powershell
git clone https://github.com/xpatto/el-buen-sabor.git
Set-Location el-buen-sabor
java -version
javac -version
.\mvnw.cmd -q dependency:build-classpath '-DincludeScope=runtime' '-Dmdep.outputFile=target/local-classpath.txt'
if ($LASTEXITCODE -ne 0) { throw 'No se pudieron preparar las bibliotecas' }
$configLocal = Join-Path $env:TEMP ([guid]::NewGuid().ToString())
New-Item -ItemType Directory -Path $configLocal | Out-Null
$archivoHash = Join-Path $configLocal 'password.bcrypt'
java --class-path ((Get-Content target/local-classpath.txt -Raw).Trim()) scripts/GenerarHash.java $archivoHash
if ($LASTEXITCODE -ne 0) { throw 'No se pudo generar el hash' }
$env:ADMIN_USERNAME = Read-Host 'Usuario administrativo local'
$env:ADMIN_PASSWORD_HASH = (Get-Content $archivoHash -Raw).Trim()
Remove-Item $archivoHash
Remove-Item $configLocal
$env:SPRING_PROFILES_ACTIVE = 'dev'
.\mvnw.cmd spring-boot:run
```

El directorio temporal debe ser privado para tu cuenta: Windows hereda sus ACL;
la utilidad no administra ACL. Usa las mismas URLs que en Linux y Ctrl+C para
detener. Después elimina las variables de esta sesión:

```powershell
Remove-Item Env:ADMIN_USERNAME, Env:ADMIN_PASSWORD_HASH, Env:SPRING_PROFILES_ACTIVE
```

Pruebas (Node requerido solo para las de JavaScript):

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd package
node --test src/test/js/*.test.mjs
```

Para una base nueva, con credenciales configuradas, crea un directorio temporal
como arriba y asigna `$env:APP_DB_PATH = Join-Path $configLocal 'menu.db'` antes
de iniciar. Después de detener, usa `Remove-Item Env:APP_DB_PATH` para volver
a la base habitual. No borres la base real.

## Problemas frecuentes

- Java distinto de 21: corregir PATH/JAVA_HOME y abrir otra terminal.
- Permiso denegado en Linux: `chmod +x mvnw` (Git conserva el bit ejecutable).
- Puerto 8080 ocupado: detener la instancia anterior; alternativamente usar
  `SERVER_PORT=8081` en el entorno y cambiar ambas URLs al puerto elegido.
- Configuración administrativa ausente/inválida: generar BCrypt y exportar ambas
  variables en la misma terminal. El hash no lleva el prefijo `{bcrypt}`.
- El generador no encuentra consola: usar una terminal real sin tuberías ni
  redirecciones de entrada/salida; la contraseña se introduce en sus prompts.
- Error al descargar dependencias: revisar conexión/proxy y reintentar Maven.
  No desactivar TLS ni validación de certificados.
- SQLite no se abre: el directorio padre de APP_DB_PATH debe existir y permitir
  escritura. Usar disco local, no una carpeta pública ni una unidad de red.
- Bases existentes conservan sus cambios. Una base temporal permite volver a las
  demostraciones sin eliminar datos. Eliminar en el panel es irreversible.

## Límites de seguridad de la entrega

No hay credenciales predeterminadas. El generador reutiliza Spring Security ya
presente, no se empaqueta y no cambia el login, las APIs ni el esquema. El hash
se conserva solo en el entorno del proceso tras borrar el archivo temporal;
el administrador del equipo y otros procesos de la misma cuenta pueden tener
acceso a ese entorno. No usar terminales compartidas ni publicar volcados.

La ausencia del enlace administrativo es una decisión de interfaz; /admin sigue
protegido por sesión, rol y CSRF. Git ignora bases SQLite, auxiliares, .env, logs,
archivos .bcrypt/.bak y carpetas backups/credentials/. Esto no impide agregar
secretos con git add -f o bajo otros nombres: revisar cada diff antes de publicar.

El cierre no certifica seguridad para Internet. La revisión iniciada de etapa
7A se interrumpió al cambiar el objetivo a entrega local: no se aplicaron cambios
de versiones, perfil prod ni respaldo operacional. Spring Boot 3.5.16 es la
última versión OSS de la línea 3.5 según el [anuncio oficial](https://spring.io/blog/2026/06/25/spring-boot-3-5-16-available-now/).
SQLite 3.49.1 es anterior a correcciones de seguridad del [motor SQLite](https://www.sqlite.org/cves.html).
Conservar esto como deuda de mantenimiento y revisar dependencias antes de
cualquier futura exposición a redes no confiables. No se afirma ausencia de CVE.


## Verificación realizada el 2026-10-11

| Comando | Ejecutadas | Fallidas | Errores | Omitidas |
| --- | ---: | ---: | ---: | ---: |
| ./mvnw clean test | 219 | 0 | 0 | 0 |
| ./mvnw package | 219 | 0 | 0 | 0 |
| node --test src/test/js/*.test.mjs | 38 | 0 | 0 | 0 |

Ambos comandos Maven: BUILD SUCCESS. Recuento contrastado con Surefire.
Son 257 casos distintos; package repite los 219 Java. Se conservan las pruebas
anteriores de API, eliminación definitiva, migración, autenticación y CSRF.

Se creó un clon local aislado desde 747bd31 y se copiaron únicamente los cambios
de esta entrega, sin data/menu.db, configuración secreta ni target del original.
Se ejecutó el comando Maven de classpath y los pasos de Fish y Bash con entradas
ficticias en una pseudoterminal. Se reutilizó la caché Maven del equipo: no se
comprobó una descarga completa con caché vacía ni un clon remoto desde GitHub.

Verificaciones adicionales realizadas (no sumadas al recuento de suites):

- Generación BCrypt coste 12 sin poner contraseña/hash en argumentos o logs;
  login real con ese hash, token extraído de Thymeleaf y cookie de sesión.
- GET / sin enlace administrativo; acceso directo /admin y APIs pública y
  administrativa tras login, contra Tomcat iniciado por Maven Wrapper.
- Creación automática de tres categorías y siete productos en la copia limpia.
- Arranque Bash con SQLite temporal, conservando la base habitual de la copia.
- Generador rechaza contraseña corta, confirmación distinta, más de 72 bytes y
  archivo de destino existente. La aplicación se detuvo mediante Ctrl+C.

El primer arranque de comprobación falló porque 8080 ya estaba ocupado. Se usó
SERVER_PORT con un puerto local libre para las verificaciones posteriores; no
se detuvo la instancia existente. No se modificó la configuración del proyecto.
Windows/PowerShell no estaba disponible: sus comandos son una alternativa
orientativa, no una validación ejecutada. Tampoco se repitieron pruebas visuales
de navegador; las pruebas manuales previas fueron confirmadas por el usuario.

SHA-256 de todos los archivos de data/ idéntico antes/después. Revisión del árbol
actual y ocho commits previos (126 blobs únicos), sin secretos reales ni bases
versionadas identificados; únicamente fixtures ficticios de pruebas. El JAR no
incluye el generador, clases de credenciales de prueba ni archivos SQLite.
La revisión abarca referencias locales disponibles, no garantiza detectar todo
secreto posible ni examina referencias remotas que no estén en este clon.

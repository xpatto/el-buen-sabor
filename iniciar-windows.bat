@echo off
setlocal EnableExtensions DisableDelayedExpansion
cd /d "%~dp0"
if errorlevel 1 goto carpeta_error
set "JAVA_DEMO=java.exe"
if defined JAVA_HOME set "JAVA_DEMO=%JAVA_HOME%\bin\java.exe"
"%JAVA_DEMO%" scripts\VerificarDemo.java
if errorlevel 1 goto requisitos_error

echo El Buen Sabor - demostracion LOCAL ^(solo 127.0.0.1^).
echo La primera ejecucion puede tardar: Maven Wrapper descarga Maven y dependencias.
echo Espera el mensaje Started MenuApplication antes de abrir:
echo Menu: http://localhost:8080/
echo Administracion: http://localhost:8080/admin ^(usuario: admin, clave: admin^)
echo Base independiente: data/menu-demo.db. Los cambios se conservan al reiniciar.
echo Detener: Ctrl+C en esta terminal.
call mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=demo "-Dspring-boot.run.arguments=--spring.profiles.active=demo --server.address=127.0.0.1 --server.port=8080 --spring.datasource.url=jdbc:sqlite:./data/menu-demo.db"
if errorlevel 1 goto arranque_error
exit /b 0

:carpeta_error
echo No se pudo acceder a la carpeta del proyecto. Extrae el ZIP en una carpeta local.
goto error
:requisitos_error
echo Se requiere JDK 21 y el puerto 8080 libre. Revisa el error anterior, PATH y JAVA_HOME.
goto error
:arranque_error
echo El servidor se detuvo. Si no fue por Ctrl+C, revisa el error anterior.
echo Comprueba JDK 21, la conexion inicial a Maven Central y el puerto 8080.
:error
pause
exit /b 1

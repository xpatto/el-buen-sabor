# Verificación del frontend — etapa 6

## Arranque local y credenciales

Requiere Java 21. Primero ejecutar ./mvnw package para disponer del JAR y las
dependencias. El perfil dev escucha únicamente en 127.0.0.1 por HTTP.

Configurar ADMIN_USERNAME y ADMIN_PASSWORD_HASH fuera del repositorio. No usar
las credenciales ficticias de las pruebas. El hash debe ser BCrypt local, con
coste 12 recomendado; no usar generadores en sitios web ni poner la contraseña
en el historial de comandos. Una opción sin dependencias nuevas es guardar
este programa temporal como /tmp/GenerarHash.java:

```java
import java.util.Arrays;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
class GenerarHash {
    public static void main(String[] args) {
        var console = System.console();
        if (console == null) throw new IllegalStateException("Ejecutar en una terminal interactiva");
        char[] password = console.readPassword("Contraseña local nueva: ");
        if (password == null || password.length == 0) throw new IllegalArgumentException("Contraseña vacía");
        try {
            System.out.println(new BCryptPasswordEncoder(12).encode(new String(password)));
        } finally {
            Arrays.fill(password, '\0');
        }
    }
}
```

Ejecutar en una terminal interactiva Fish, con las versiones resueltas por el
proyecto en esta etapa:

```fish
java --class-path "$HOME/.m2/repository/org/springframework/security/spring-security-crypto/6.5.11/spring-security-crypto-6.5.11.jar:$HOME/.m2/repository/org/springframework/spring-jcl/6.2.19/spring-jcl-6.2.19.jar" /tmp/GenerarHash.java
read --prompt-str 'Usuario local: ' ADMIN_USERNAME
read --silent --prompt-str 'Hash BCrypt generado: ' ADMIN_PASSWORD_HASH
set -gx ADMIN_USERNAME $ADMIN_USERNAME
set -gx ADMIN_PASSWORD_HASH $ADMIN_PASSWORD_HASH
./mvnw spring-boot:run
```

El hash también es sensible: no compartir la salida de esa terminal ni guardarla
en Git. Al terminar, detener el servidor y borrar las variables con
`set -e ADMIN_USERNAME ADMIN_PASSWORD_HASH`. No usar el perfil dev en Internet.

Para comprobaciones manuales que modifiquen productos, usar una base desechable:

```fish
set -l prueba_dir (mktemp -d)
env APP_DB_PATH="$prueba_dir/menu.db" ./mvnw spring-boot:run
```

Menú: http://localhost:8080/. Administración: http://localhost:8080/admin.
No ejecutar las pruebas manuales de altas/edición sobre datos que se deseen conservar.

## Pruebas automatizadas

```sh
./mvnw clean test
./mvnw package
node --test src/test/js/*.test.mjs
```

Node se usa solo para las pruebas ligeras; no se instala ningún paquete npm.
Se verificó con Node 26.11.1. Maven no ejecuta las pruebas JavaScript: son una
comprobación separada. Los reportes Java están en target/surefire-reports.

Spring verifica recursos públicos enumerados, rutas desconocidas denegadas,
login/panel condicional, token AJAX utilizable y logout protegido, CSP y todas
las regresiones anteriores de API, SQLite y producción. JavaScript verifica
precios e IDs exactos, límites, serialización, cookies del mismo origen, CSRF,
errores HTTP/red y rechazo de respuestas inválidas sin reintentar escrituras.

## Lista manual — Firefox en CachyOS

Esta lista no equivale a una ejecución realizada. Usar una base desechable y
anotar versión del navegador, fecha y resultado de cada punto.

- [ ] Abrir / sin sesión: Todos seleccionado, tarjetas completas y precios MXN.
- [ ] Filtrar cada categoría y volver a Todos sin recarga; cambiar rápidamente
  de categoría y comprobar que prevalece la última selección.
- [ ] Con la red deshabilitada en herramientas de desarrollo, cambiar el filtro:
  aparece error y reintento. Restaurar la red y recuperar el catálogo.
- [ ] Ocultar todos los productos de una categoría en el panel y verificar su
  estado vacío en el menú; después reactivarlos.
- [ ] Abrir /admin sin sesión, comprobar login incorrecto y login correcto.
- [ ] Agregar un producto y comprobar listado administrativo y menú público.
- [ ] Editar nombre, descripción, categoría y precio desde el listado.
- [ ] Ocultar, editar el producto oculto y comprobar que sigue oculto; reactivar.
- [ ] Probar vacíos, solo espacios, nombres de 121 caracteres, descripciones de
  2001, precio cero/negativo, coma y tres decimales: no deben guardarse.
- [ ] Crear con 92233720368547758.07; editar solo el nombre y verificar que el
  precio conserve exactamente sus centavos. Rechazar 92233720368547758.08.
- [ ] Introducir texto como <img src=x onerror=alert(1)> en un nombre: debe
  mostrarse como texto, nunca ejecutarse ni crear una imagen.
- [ ] Guardar con red interrumpida: conservar los campos y revisar el listado
  antes de repetir, porque el servidor pudo haber confirmado la escritura.
- [ ] Cerrar sesión en otra pestaña; intentar actualizar el panel anterior:
  debe avisar, bloquear escrituras y ofrecer volver al login.
- [ ] Usar Cerrar sesión normalmente y confirmar regreso al formulario.
- [ ] Recorrer con Tab/Shift+Tab, activar con teclado, verificar foco visible,
  etiquetas y notificaciones. Comprobar zoom 200 % y ancho móvil de 390 px.
- [ ] Revisar consola: sin errores de módulos ni bloqueos CSP de recursos propios;
  ningún token, contraseña o cookie en localStorage.

## Evidencia de navegador y límites

En la sesión de implementación se ejecutaron 14 escenarios automatizados en
Chromium 153.0.8010.52, contra Tomcat real con credenciales y SQLite temporales:
menú inicial, filtro, ancho móvil, error de red/reintento, login, validación,
alta exacta del precio máximo con texto HTML seguro, edición exacta, ocultación,
edición de oculto, reactivación, panel móvil/almacenamiento, sesión invalidada y
logout. Los 14 pasaron, sin errores JavaScript no controlados. Se inspeccionaron
capturas de escritorio y móvil. No se ejecutó la lista manual de Firefox.

La interfaz requiere JavaScript moderno con módulos, fetch y BigInt. No hay
modo offline ni actualizaciones en tiempo real entre pestañas: usar Actualizar
listado. Las ediciones simultáneas conservan los límites SQLite documentados en
API-ADMINISTRATIVA.md. La etapa 6 conservó los contratos; la ampliación 6.1 añade
DELETE y metadatos de inicialización, manteniendo el formato JSON de los productos.
Antes de publicar en Internet siguen aplicando los pendientes de SEGURIDAD.md.

## Cierre de etapa 6 verificado el 2026-10-11

| Comprobación | Ejecutadas | Aprobadas | Fallidas | Errores | Omitidas |
| --- | ---: | ---: | ---: | ---: | ---: |
| ./mvnw clean test | 191 | 191 | 0 | 0 | 0 |
| ./mvnw package | 191 | 191 | 0 | 0 | 0 |
| node --test src/test/js/*.test.mjs | 34 | 34 | 0 | 0 | 0 |

Ambos comandos Maven finalizaron con BUILD SUCCESS. El recuento Java se
contrastó con los XML de Surefire; las pruebas JS se ejecutaron por separado.
Los 14 escenarios Chromium descritos arriba se ejecutaron en la sesión inicial
de implementación, no en esta repetición del 11 de octubre. Firefox manual
permanece pendiente; no se declara aprobado.

SHA-256 de todos los archivos de data/ idéntico antes y después de las pruebas
finales; data/menu.db no existía y no fue creado. Revisión de los 73 archivos
candidatos a Git sin secretos reales identificados ni bases SQLite incluidas.
Git diff --check sin incidencias. No se realizaron cambios remotos.

Problemas resueltos: tokenización de JSON con ceros iniciales inválidos y
prueba de bienvenida MockMvc adaptada a su forward interno; la lectura real
de la página raíz se comprobó además en Chromium. No se modificaron contratos
ni validaciones del backend para resolver la precisión monetaria.

## Lista manual adicional — etapa 6.1

Ejecutar únicamente con SQLite desechable. Eliminar es irreversible; Ocultar
sigue siendo reversible. Estos puntos están preparados, no marcados como
pruebas de navegador ejecutadas para esta ampliación.

- [ ] Pulsar Eliminar: comprobar nombre y advertencia. Cancelar y verificar
  en Red que no se envió DELETE y que el producto permanece.
- [ ] Confirmar: verificar DELETE con X-CSRF-TOKEN, respuesta 204 vacía,
  controles bloqueados durante el envío y mensaje de éxito.
- [ ] Actualizar el menú público: el producto eliminado ya no aparece.
- [ ] Eliminar uno de los siete productos iniciales, reiniciar y comprobar
  que sigue ausente y los demás conservan sus datos.
- [ ] Eliminar todos, reiniciar y comprobar el catálogo vacío; dar de alta
  un producto nuevo y comprobar que se puede seguir administrando.
- [ ] Ocultar y reactivar otro producto: ambas acciones siguen funcionando.
- [ ] Interrumpir la red al eliminar: aparece aviso sin repetición automática;
  actualizar el listado antes de repetir.
- [ ] Comprobar teclado, confirmación nativa accesible y botón de peligro en
  escritorio y móvil; cerrar el editor si se elimina el producto editado.

Las pruebas Node comprueban cancelación sin petición ni bloqueo, confirmación
con nombre e irreversibilidad, ID largo exacto, token y ausencia de reintentos.
requestJson acepta 204 sin consultar el cuerpo. Las pruebas Java usan bases
temporales para migración, borrado, reinicio, permisos, CSRF y regresiones.


## Cierre automatizado de etapa 6.1 — 2026-10-11

| Comprobación | Ejecutadas | Aprobadas | Fallidas | Errores | Omitidas |
| --- | ---: | ---: | ---: | ---: | ---: |
| ./mvnw clean test | 204 | 204 | 0 | 0 | 0 |
| ./mvnw package | 204 | 204 | 0 | 0 | 0 |
| node --test src/test/js/*.test.mjs | 38 | 38 | 0 | 0 | 0 |

Ambos comandos Maven terminaron con BUILD SUCCESS. Los XML de Surefire
confirman el recuento Java. Son 242 pruebas distintas de Java y JavaScript;
package repite las 204 Java, no añade casos distintos.

En esta ampliación data/menu.db sí existía: su SHA-256 y el de los demás
archivos de data/ son idénticos al estado previo. No se borraron productos
de desarrollo. La prueba de regresión detectó que SQLite reutilizaba el mayor
ID eliminado; el contador persistente lo evita también tras vaciar y reiniciar.

La cobertura automatizada nueva es Java/Node. No se ejecutaron escenarios
Chromium ni Firefox adicionales para la etapa 6.1; la lista manual anterior
permanece pendiente. No confundirla con los 14 escenarios históricos de etapa 6.

Revisión de los 75 archivos candidatos a Git: sin secretos reales identificados,
claves privadas, hashes de producción ni bases de datos incluidas. Las únicas
coincidencias de credenciales son valores ficticios de pruebas. git diff --check
sin incidencias. Entrega mediante commit local, sin push ni despliegue.

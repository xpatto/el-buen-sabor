# Seguridad — configuración vigente

## Configuración

Se requieren `ADMIN_USERNAME` (no vacío ni con espacios exteriores) y
`ADMIN_PASSWORD_HASH` en todos los perfiles normales (incluidos dev y prod). El hash debe tener formato BCrypt
$2a$, $2b$ o $2y$, sin prefijo `{bcrypt}`. No se aceptan contraseñas en texto
plano como configuración ni hay usuario predeterminado en esos perfiles. Un hash sintácticamente
válido no demuestra que el operador conozca su contraseña: verificar el acceso
antes de publicar. El encoder usa coste 12 al generar; se admiten costes BCrypt
4–31 por compatibilidad. Para producción generar con una herramienta local
confiable, coste 12 o superior ajustado a la máquina, y contraseña larga y única.
No incluir contraseña/hash en comandos guardados, Git, capturas ni logs.
Las variables deben inyectarse mediante el gestor de secretos del despliegue.

`dev` es el perfil predeterminado, HTTP en 127.0.0.1; no usarlo en Internet.
Seleccionar explícitamente `SPRING_PROFILES_ACTIVE=prod` en producción.
Configurar `APP_DB_PATH` en un directorio persistente con permisos restringidos.

## Demostración local

La única excepción explícita es `demo`, para evaluar el proyecto con `admin/admin`.
El hash se genera en memoria mediante el PasswordEncoder BCrypt existente;
no se guarda un hash ni se modifica el login. Se mantienen ROLE_ADMIN, sesiones,
CSRF, CSP y las rutas protegidas. Las cookies locales son HttpOnly y SameSite=Lax;
Secure permanece desactivado para HTTP local, igual que en dev.

Antes de instanciar SQLite o Tomcat se valida que demo sea el único perfil,
que la dirección efectiva sea exactamente 127.0.0.1 y que las propiedades de
conexión apunten exclusivamente a `data/menu-demo.db`. Se comprueba también
la dirección final de la fábrica Tomcat. Se rechazan perfiles mezclados,
direcciones alternativas y URLs de base diferentes, incluida la propiedad
específica Hikari. APP_DB_PATH no selecciona la base demo. No se admiten enlaces
del archivo demo a otra base ni un archivo compartido con menu.db.

Las credenciales demo son públicas, deliberadas y exclusivas de este perfil.
Cualquier usuario local que pueda acceder al puerto puede utilizarlas; no guardar
datos sensibles en la demostración ni exponerla con túneles, proxies o reenvíos.
El enlace administrativo oculto no es una barrera de seguridad: /admin conserva
su autenticación. Para datos propios usa dev con credenciales únicas.

Windows usa el mismo Maven Wrapper 3.3.4 y Maven 3.9.11 mediante Java, sin
PowerShell. `scripts/PrepararMaven.java` descarga el JAR oficial desde Maven
Central por HTTPS, verifica su SHA-256 fijado en el código y también comprueba
la copia guardada antes de usarla. El JAR está ignorado por Git; no es un secreto.
No se modifican versiones ni dependencias de la aplicación.

## HTTPS y proxy

Producción redirige HTTP a HTTPS en el puerto público 443, independientemente
del puerto interno de Tomcat. Solo se interpretan X-Forwarded-Proto y
X-Forwarded-For desde direcciones coincidentes con `TRUSTED_PROXY_IP_REGEX`.
El valor predeterminado no confía en ninguna IP; no habilitar rangos amplios.
X-Forwarded-Host y X-Forwarded-Port están deshabilitados; Forwarded no se utiliza.
El proxy debe validar/conservar Host, eliminar encabezados del cliente y
establecer sus propios encabezados de protocolo/IP. Debe terminar TLS con un
certificado válido y bloquear el acceso externo directo al puerto de Tomcat.

Las pruebas usan conexiones HTTP locales a Tomcat real y simulan el salto
proxy–aplicación. Verifican Location con :443; no abren un servidor TLS público
ni validan un certificado o un proxy desplegado.

## Autenticación y sesión

Un administrador en memoria con rol ADMIN. GET /admin presenta el formulario
Thymeleaf o el panel de productos autenticado. POST /admin/login y
POST /admin/logout exigen CSRF. La autenticación cambia el ID de sesión;
logout invalida la sesión. No hay HTTP Basic ni registro público. La etapa 5
incorpora la API de gestión de productos descrita en API-ADMINISTRATIVA.md.
GET /api/categorias y /api/productos siguen públicos. /api/admin/** exige ADMIN;
401 JSON sin sesión, 403 sin rol, 404 para rutas aún no implementadas con ADMIN.
Las demás rutas se deniegan salvo los GET públicos expresamente enumerados.

Cookies de sesión: HttpOnly, SameSite=Lax, Secure en prod; caducidad por
inactividad de 30 minutos y transporte solo por cookies. El perfil dev omite
Secure para permitir HTTP local. CSP restringida, X-Frame-Options DENY,
nosniff, no-store y HSTS de un año en HTTPS (sin includeSubDomains).
Errores de credenciales genéricos, sin revelar usuario existente o valores
configurados; errores HTTP sin trazas internas.

## Evidencia y límites

La suite comprueba el token CSRF extraído del HTML Thymeleaf mediante login
real contra Tomcat, la rotación de sesión y cookies antes/después del login;
redirecciones a :443 con y sin proxy confiable; cabeceras falsificadas desde
origen no confiable; API pública y rutas administrativas. Pruebas de arranque
Boot completo con prod rechazan credenciales ausentes o inválidas. MockMvc
comprueba CSRF faltante/incorrecto, logout y rol insuficiente, entre otros casos.
Todas las bases de pruebas son independientes de la base de desarrollo.

La revisión de Git debe distinguir el fixture ficticio TestAdminCredentials,
exclusivo de src/test, de credenciales reales. Su hash se genera en ejecución;
no se empaqueta en el JAR. No se versionan secretos ni bases SQLite.

## Antes de publicar en Internet

- Configurar y probar TLS, dominio/Host permitido, proxy y firewall reales;
  impedir cabeceras heredadas del cliente y acceso directo a Tomcat.
- Implementar limitación de intentos de login y de solicitudes en el proxy,
  con monitorización: la aplicación todavía no limita fuerza bruta ni volumen.
- Aprovisionar credenciales únicas, proteger y rotar secretos, restringir
  permisos de SQLite y verificar respaldos/restauración.
- Revisar vulnerabilidades de dependencias y actualizar con pruebas antes
  del despliegue; esta etapa no constituye una auditoría de CVE ni pentest.
- Las sesiones son locales en memoria: un reinicio requiere nuevo login;
  no se ha diseñado un despliegue con múltiples instancias.

Estos puntos operativos siguen pendientes; no autorizan despliegue ni etapa 7.

## Resultados finales de etapa 4 (2026-10-09)

| Comando | Ejecutadas | Fallidas | Errores | Omitidas | Resultado |
| --- | ---: | ---: | ---: | ---: | --- |
| `./mvnw clean test` | 113 | 0 | 0 | 0 | BUILD SUCCESS |
| `./mvnw package` | 113 | 0 | 0 | 0 | BUILD SUCCESS |

Son 113 casos, ejecutados en ambas verificaciones (226 ejecuciones en total).
Recuento contrastado con los XML de `target/surefire-reports/`. El JAR generado
no incluye TestAdminCredentials. Revisión manual y búsqueda de patrones sobre
los 55 archivos candidatos a Git: sin secretos reales identificados; solo datos
ficticios de prueba y nombres de variables. Esto no sustituye una auditoría.

La primera ejecución aislada quedó detenida en Mockito y se canceló; las
verificaciones finales se ejecutaron con acceso a JVM y puertos locales.
Las pruebas reforzadas detectaron aceptación de X-Forwarded-Host desde el
proxy confiable; se deshabilitaron host/puerto reenviados y la regresión pasó.
También se corrigió la comparación para admitir 443 implícito o explícito.
No se verificó TLS de una infraestructura desplegada. Sin CRUD ni etapa 5.

## Frontend y CSP (etapa 6)

Se permite script-src 'self' para módulos JavaScript locales; se mantienen
default-src 'self', style-src 'self', object-src 'none', base-uri 'none',
form-action 'self' y frame-ancestors 'none'. No se añade unsafe-inline,
unsafe-eval, fuentes externas ni CORS. Permisos GET explícitos para
/css/styles.css, /css/admin.css, /js/app.js, /js/admin.js y /js/catalog.js;
el resto de rutas desconocidas continúa denegado.

El panel autenticado expone el token CSRF vigente en una metaetiqueta mediante
th:content; admin.js lo envía en X-CSRF-TOKEN para cada escritura. Login y
logout conservan sus formularios con CSRF. Fetch utiliza credentials:
same-origin y no reintenta escrituras automáticamente. Ante 401/403 se
deshabilitan las acciones y se ofrece volver a iniciar sesión.

La API no se inserta como HTML: se usan createElement y textContent, sin
innerHTML. Los textos se serializan con JSON.stringify; solo números validados
se incorporan como literales JSON para conservar precisión decimal. No hay
tokens, cookies ni contraseñas en localStorage. Descargar admin.js es público
y no expone datos del catálogo administrativo ni credenciales.

La CSP reduce el riesgo de inyección, pero no sustituye validaciones,
actualizaciones ni la configuración segura del despliegue. Una sesión vencida
se detecta en la próxima solicitud; el listado ya mostrado puede permanecer
visible hasta recargar o navegar. Usar Cerrar sesión al terminar, especialmente
en equipos compartidos. Persisten los pendientes operativos indicados arriba.

Cierre de etapa 6 (2026-10-11): clean test y package aprobaron 191 pruebas
cada uno, sin fallidas, errores ni omitidas; 34 pruebas JavaScript aprobadas
por separado. En la sesión de implementación pasaron 14 escenarios Chromium.
La lista manual Firefox está preparada y pendiente de ejecución. Datos de
desarrollo intactos por SHA-256. Detalles y límites en PRUEBAS-FRONTEND.md.


## Cierre para ejecución local — 2026-10-11

El alcance vigente se limita a clonar y ejecutar localmente, sin despliegue.
El Inicio rápido del README genera BCrypt con scripts/GenerarHash.java usando
entrada de consola sin eco y archivo temporal privado; no incluye secretos
predeterminados. Usar dev y loopback. No compartir archivos ni entorno del proceso.

Se revisaron los ocho commits alcanzables por las referencias Git locales
(126 blobs únicos) y los archivos de esta entrega: no se identificaron secretos
reales, hashes BCrypt persistidos, claves privadas ni bases de datos versionadas.
Las coincidencias fueron credenciales ficticias exclusivas de tests. Esta revisión
por patrones y lectura no equivale a una auditoría exhaustiva ni verifica ramas
remotas ausentes del clon. No se cambió la visibilidad ni se realizó push.

Se añadieron comprobaciones de denegación para 15 rutas de SQLite, auxiliares,
respaldos, credenciales, logs y configuración: 401 anónimo y 403 incluso con ADMIN.
El JAR no incluye TestAdminCredentials, el generador ni archivos SQLite/secretos.
Resultados y límites de dependencias en [EJECUCION-LOCAL.md](EJECUCION-LOCAL.md).
Los requisitos de Internet anteriores se conservan solo como referencia futura.

## Verificación del modo demo — 2026-10-11

`clean test` y `package`: 245 pruebas Java aprobadas por comando, 0 fallidas,
0 errores y 0 omitidas. Node: 38 aprobadas, 0 fallidas, 0 omitidas ni canceladas.
Los 22 casos Java nuevos comprueban el modo demo y su bootstrap; la regresión
conserva las pruebas de credenciales inválidas en prod, HTTPS, proxy y cookies.

Tomcat real aceptó admin/admin con el token del formulario y rechazó login
incorrecto, API administrativa sin sesión y escrituras sin CSRF. CRUD, cambio
de estado y logout permanecieron operativos. Dev, prod y el perfil por defecto
rechazaron credenciales ausentes. Se rechazaron binds alternativos, perfiles
mezclados/incluidos y cambios de conexión SQLite antes de abrir la base demo.
La base normal ficticia conservó sus bytes; las ediciones y eliminaciones demo
sobrevivieron al reinicio. Los archivos originales de data/ no se modificaron.

La revisión de archivos candidatos a Git no identificó secretos reales ni
bases versionadas: admin/admin es la credencial pública autorizada exclusivamente
para demo; el SHA-256 del wrapper identifica un artefacto público. No se publica
su JAR descargado ni credenciales de dev/prod. No cambió SecurityConfig, el
esquema, las APIs ni la interfaz. El bootstrap Java fue ejecutado en Linux;
Windows 10 real sigue pendiente. El modo demo no habilita exposición a Internet.

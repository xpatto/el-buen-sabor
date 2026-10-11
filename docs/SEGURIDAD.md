# Seguridad — configuración vigente hasta etapa 6

## Configuración

Se requieren `ADMIN_USERNAME` (no vacío ni con espacios exteriores) y
`ADMIN_PASSWORD_HASH` en todos los perfiles. El hash debe tener formato BCrypt
$2a$, $2b$ o $2y$, sin prefijo `{bcrypt}`. No se aceptan contraseñas en texto
plano como configuración ni hay usuario predeterminado. Un hash sintácticamente
válido no demuestra que el operador conozca su contraseña: verificar el acceso
antes de publicar. El encoder usa coste 12 al generar; se admiten costes BCrypt
4–31 por compatibilidad. Para producción generar con una herramienta local
confiable, coste 12 o superior ajustado a la máquina, y contraseña larga y única.
No incluir contraseña/hash en comandos guardados, Git, capturas ni logs.
Las variables deben inyectarse mediante el gestor de secretos del despliegue.

`dev` es el perfil predeterminado, HTTP en 127.0.0.1; no usarlo en Internet.
Seleccionar explícitamente `SPRING_PROFILES_ACTIVE=prod` en producción.
Configurar `APP_DB_PATH` en un directorio persistente con permisos restringidos.

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

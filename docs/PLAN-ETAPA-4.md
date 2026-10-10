# Plan de implementación: seguridad y autenticación

Especificación: solicitud de etapa 4 y [ALCANCE.md](ALCANCE.md).
Se mantienen Java 21, Boot 3.5.16, el esquema SQLite y la API pública.

## Diseño

- Configuración de credenciales separada de la cadena web: un usuario en memoria,
  variables `ADMIN_USERNAME` y `ADMIN_PASSWORD_HASH`, validación BCrypt y fallo
  de arranque con mensajes genéricos. No hay valores predeterminados.
- `SecurityConfig`: lista explícita de GET públicos, POST de acceso y salida;
  `/api/admin/**` requiere ADMIN; resto denegado. Sin HTTP Basic ni CORS global.
- Login y logout procesados por Security, CSRF activo, cambio de ID de sesión,
  sin caché de peticiones que provoque redirecciones de clientes API.
- Controlador MVC y `templates/admin.html`: formulario anónimo o estado
  provisional y logout; sin JavaScript ni tercera página.
- Errores del filtro con `ApiError`, 401/403 JSON; los errores MVC mantienen
  el manejador existente y las rutas administrativas no implementadas dan 404
  después de superar autorización.
- Sesión de 30 minutos, HttpOnly, SameSite=Lax y solo cookies para transportar
  sesiones. Perfil dev local HTTP; prod HTTPS, Secure, HSTS y cabeceras del proxy
  procesadas por Tomcat únicamente desde IPs expresamente confiables.

## Ejecución

- [x] Añadir las tres dependencias autorizadas y fixtures exclusivos de tests.
- [x] Escribir pruebas de acceso, CSRF, sesión, logout, errores y cabeceras.
- [x] Implementar credenciales, cadena de seguridad y página administrativa.
- [x] Probar fallo de arranque sin configuración válida y cookies/proxy de prod
  con servidor embebido real, usando SQLite temporal.
- [x] Adaptar solo expectativas afectadas por seguridad en pruebas anteriores;
  actualizar documentación y revisar cambios.
- [x] Ejecutar `./mvnw clean test` y `./mvnw package`, comunicar resultados y
  crear commit local sin push.

## Riesgos a comprobar

Login/logout sin CSRF; sesión fijada; acceso anónimo o sin rol; rutas desconocidas;
cabeceras Forwarded falsificadas; credenciales ausentes o hash inválido; filtrado
de secretos en respuestas/logs; cookies de producción sin Secure. El despliegue,
la limitación de intentos y el proxy real se documentan como pendientes.

Verificación final: 113 pruebas por comando, 0 fallidas, errores u omitidas.
Resultados y límites en SEGURIDAD.md. Commit exclusivamente local.

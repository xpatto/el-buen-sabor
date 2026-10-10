# Plan de implementación: persistencia SQLite

Especificación: etapa 2 solicitada y [ALCANCE.md](ALCANCE.md).
Ejecución en la carpeta actual; commit local después de verificar, sin push.

## Diseño

Spring JDBC y SQLite JDBC con versiones administradas por Spring Boot 3.5.16.
Hikari configura las claves foráneas en cada conexión física. Ruta por defecto
`./data/menu.db`, configurable mediante `APP_DB_PATH`. El directorio `data/`
se conserva en Git mediante `.gitkeep`, sin versionar bases de datos.

`Categoria` y `Producto` serán records en `domain`, con identificadores y
precios en `long` y estado en `boolean`. Los repositorios en `repository`
ofrecerán `listarTodas()`, `listarActivos()` y
`listarActivosPorCategoria(String slug)`, con orden estable por identificador.
No se añaden servicios sin comportamiento ni operaciones de escritura.

Los scripts crean tablas con `IF NOT EXISTS` e insertan semillas con IDs
estables y `ON CONFLICT ... DO NOTHING`. No se usan reemplazos, actualizaciones
ni borrados. Los IDs iniciales quedan reservados a estas semillas; ocultar un
producto no lo vuelve a activar al reiniciar. El borrado físico no es parte
del alcance aprobado.

## Pasos y comprobaciones

- [x] Añadir dependencias e infraestructura de pruebas aislada. Comprobar fallo
  por ausencia de configuración/tablas antes de implementar SQLite.
- [x] Crear `application.properties`, `schema.sql`, `data.sql` y `data/.gitkeep`.
  Probar conexión real, 3 categorías, 7 productos, integridad, claves foráneas
  en varias conexiones y reinicialización conservando ediciones y datos extra.
- [x] Probar e implementar modelos y repositorios. Verificar datos exactos,
  filtros, exclusión de inactivos y parámetros desconocidos o maliciosos.
- [x] Actualizar README y alcance, revisar cambios y ejecutar
  `./mvnw clean test` y `./mvnw package`. Comunicar resultados y crear commit.

## Casos de revisión

Probar precios no enteros o no positivos, campos obligatorios, estados fuera
de 0/1, categorías inexistentes y slugs duplicados. Reinicializar tras editar
precio, nombre, descripción, categoría y estado. Asegurar que las conexiones
reutilizadas o nuevas mantienen las claves foráneas. Todas las pruebas Spring
heredan una URL dinámica bajo `target/`, con prioridad sobre el entorno.

## Resultado de la verificación

`./mvnw clean test` y `./mvnw package`: BUILD SUCCESS, 33 pruebas en cada
ejecución, sin fallos, errores ni omisiones. Incluyen una prueba de reinicio
real del contexto con archivo temporal y sustitución de `APP_DB_PATH`.
La revisión independiente no detectó problemas importantes. Se comprobaron
las exclusiones de Git; `data/` contiene únicamente `.gitkeep`.
Mockito emite advertencias de carga dinámica de agentes en Java 21,
sin fallos de prueba. No se efectuó push.

# Alcance de El Buen Sabor

## Requisitos originales

- Menú digital sencillo de una o máximo dos páginas para el restaurante.
- Selector desplegable: Todos, Comida / Lonchería, Cafetería y Bebidas,
  Tienda y Snacks.
- Productos con nombre, categoría, precio base y descripción o variación.
- Interfaz limpia, legible y responsiva.
- API REST pública para obtener productos y categorías.

## Ampliaciones aprobadas

- Etapa 6.1: eliminación física e irreversible de productos, conservando
  ocultación/reactivación, con inicialización persistente sin reapariciones.
- Segunda página administrativa protegida para agregar y modificar productos,
  actualizar precios, ocultar y reactivar productos.
- Persistencia SQLite mediante Spring JDBC y SQLite JDBC.
- Spring Security con sesiones HTTP y protección CSRF para el administrador.
- Validation y Thymeleaf se incorporarán cuando los módulos los requieran.

## Etapa 1 completada: inicialización

Java 21, Spring Boot 3.5.16, Maven Wrapper, coordenadas `com.elbuensabor:menu`
y paquete `com.elbuensabor.menu`. Solo Spring Web y Spring Boot Starter Test.
Clase principal, prueba de contexto, página temporal, reglas de colaboración,
documentación y primer commit local en `main`.

La arquitectura futura será un monolito por capas, separando controladores,
servicios y repositorios conforme se implementen. No se crean capas vacías.
La página temporal se conservó hasta su sustitución por el menú en la etapa 6.

## Etapa 2 completada: modelo de datos y persistencia SQLite

- Spring JDBC y SQLite JDBC de Xerial, con versiones administradas por Boot.
- `application.properties`: SQLite en `./data/menu.db`, configurable mediante
  `APP_DB_PATH`, inicialización SQL y claves foráneas en cada conexión del pool.
- Tablas `categorias` y `productos`, precios en centavos enteros positivos,
  estado activo 0/1, campos obligatorios y relación mediante clave foránea.
- Tres categorías y siete productos de demostración con precios ilustrativos,
  no información comercial real. Semillas con IDs estables, sin duplicar,
  sobrescribir ediciones ni borrar datos al reiniciar.
- Modelos inmutables `Categoria` y `Producto`. Repositorios de lectura con
  `JdbcTemplate`, `RowMapper` y filtro parametrizado por slug.
- Pruebas con bases independientes bajo `target/` o temporales; nunca se usa
  la base de desarrollo para probar ni se versionan archivos SQLite.

## Etapa 3 completada: API REST pública de solo lectura

- Servicios `CategoriaService` y `ProductoService` sobre los repositorios
  existentes, sin modificar el modelo de datos ni añadir dependencias.
- `GET /api/categorias`: arreglo con `id`, `nombre` y `slug`.
- `GET /api/productos`: activos con `id`, `nombre`, `categoria` legible,
  `precio` decimal exacto mediante `BigDecimal` y `descripcion`.
- Filtro opcional `categoria` por slug validado desde SQLite. Parámetro ausente
  devuelve todos los activos; vacío, desconocido o repetido devuelve 400.
  Una categoría existente sin productos activos devuelve 200 con arreglo vacío.
- DTO de respuesta y manejo centralizado en `ApiExceptionHandler`; los errores
  inesperados devuelven 500 sin detalles internos.
- Pruebas unitarias y MockMvc, con integración SQLite aislada y conservación
  de las pruebas anteriores.

La etapa 3 no incorporó operaciones de escritura ni interfaces definitivas.

## Etapa 4 completada: seguridad y autenticación

- Spring Security y Thymeleaf; administrador en memoria con BCrypt y
  credenciales externas obligatorias, sin valores predeterminados.
- Formulario /admin, login/logout con CSRF, rotación del ID de sesión,
  errores genéricos y API pública de lectura conservada.
- /api/admin/** exige ADMIN; el resto no autorizado se deniega.
- Perfil dev local y prod con HTTPS público 443, proxy confiable explícito,
  cookies Secure/HttpOnly/SameSite=Lax y cabeceras de seguridad.
- Pruebas MockMvc y Tomcat real con SQLite aislado. Resultados de cierre
  en README.md y SEGURIDAD.md.
- La entrega de etapa 4 se limitó al commit local, sin CRUD administrativo
  ni avance a etapa 5. El despliegue público requiere resolver los riesgos
  operativos documentados en SEGURIDAD.md.

## Etapa 5 completada: API administrativa

- GET /api/admin/productos lista activos e inactivos por identificador.
- POST registra productos activos y responde 201 con Location.
- PUT /api/admin/productos/{id} edita nombre, descripción, categoría y precio
  sin modificar activo; PATCH /{id}/estado modifica exclusivamente activo.
- AdminProductoController y DTO específicos sobre ProductoService y
  ProductoRepository existentes, sin duplicar servicios/repositorios.
- Validation para campos obligatorios y límites (nombre 120, descripción 2000).
  Precio BigDecimal positivo con máximo dos decimales y conversión exacta a
  long en centavos; se rechazan desbordamientos, IDs inválidos y categorías
  inexistentes. Estado JSON estrictamente booleano.
- JdbcTemplate parametrizado, transacciones y SQLite INTEGER PRIMARY KEY con
  INSERT RETURNING; esquema y semillas sin modificaciones.
- ROLE_ADMIN y CSRF conservados. API pública y formulario provisional intactos.
- Pruebas unitarias, integración SQLite, reinicios, rollback, seguridad y
  regresión del contrato público; datos de desarrollo protegidos.
- Documentación del contrato en API-ADMINISTRATIVA.md. Solo commit local tras
  pruebas. Sin push, panel definitivo, DELETE, despliegue ni etapa 6.

## Etapa 6 completada: frontend funcional

- Exactamente dos páginas: index.html público y /admin con login o panel
  autenticado según la sesión. No se incorporan nuevos endpoints de API.
- Menú responsivo con categorías desde SQLite, Todos inicial, filtro por slug,
  tarjetas con precios MXN y estados de carga, vacío, error y reintento.
- Panel con listado completo, altas, edición a partir del listado y acción
  independiente para ocultar/reactivar. Sin edición de categorías ni DELETE.
- HTML5, CSS Grid/Flexbox y JavaScript sin frameworks ni dependencias nuevas.
  styles.css y admin.css; app.js y admin.js; catalog.js comparte precisión y fetch.
- Validaciones de formulario equivalentes a los límites del servidor. Los
  precios e IDs long se conservan como texto exacto antes de JSON.parse; se
  envían como números JSON validados, sin redondeo mediante Number.
- CSRF desde metaetiqueta Thymeleaf, cookies del mismo origen, logout protegido,
  aviso de sesión vencida y ausencia de credenciales/tokens en almacenamiento local.
- CSP script-src 'self' y permiso GET para cinco recursos propios enumerados;
  se mantienen las demás protecciones y la autorización de /api/admin/**.
- Pruebas Spring/JavaScript, comprobaciones en navegador sobre SQLite temporal
  y lista manual para Firefox en CachyOS. Solo commit local; sin push,
  despliegue ni avance a etapa 7.

## Etapa 6.1: eliminación definitiva de productos

- DELETE /api/admin/productos/{id}, ROLE_ADMIN y CSRF; 204 sin cuerpo,
  404 inexistente y 400 ID inválido. SQL parametrizado y transacción del servicio.
- Botón Eliminar diferenciado y confirmación con nombre; cancelar no envía
  peticiones. Controles bloqueados durante el envío y notificaciones accesibles.
- requestJson acepta 204 sin leer JSON; no reintenta eliminaciones fallidas.
- inicializacion_catalogo identifica bases antiguas antes de crear tablas,
  conserva vacíos/ediciones y carga semillas una sola vez en bases nuevas.
- Contador persistente para no reutilizar IDs borrados desde la migración.
- Sin eliminación de categorías, VACUUM automático, nuevas dependencias,
  push, despliegue ni avance a etapa 7.

## Ajuste de interfaz posterior a la etapa 6.1

El ajuste de interfaz posterior a la etapa 6.1 elimina del HTML público el
enlace «Administración». Se conserva el acceso directo a /admin, su login,
panel y operaciones, sin cambios de Spring Security, rutas ni CSS.

Verificado el 2026-10-11: ./mvnw clean test y ./mvnw package finalizaron con
BUILD SUCCESS, 204 pruebas cada uno; Node aprobó sus 38 pruebas. Sin fallidas,
errores ni omitidas. FrontendTests comprueba la ausencia del enlace en la
respuesta pública y mantiene las comprobaciones de acceso directo a /admin,
login y panel. Entrega mediante commit local, sin push ni despliegue.

## Cierre final para ejecución local

El cierre final posterior a 747bd31 se limita a entrega y revisión pública del
código para ejecución local: inicio rápido, generación segura de BCrypt,
pruebas desde copia aislada y revisión de archivos/historial Git. Sustituye la
preparación 7A solicitada antes; no incluye hosting, dominio ni infraestructura.

## Variaciones simples en las descripciones iniciales

Las bases nuevas incluyen «Disponibles con o sin picante.» en Chilaquiles
verdes (ID 2) y «Opción con leche regular o leche sin lactosa.» en Licuado de
fresa (ID 4). Son texto de la descripción en las tarjetas, sin selectores ni
pedidos. Se conservan nombres, IDs, precios, categorías y carga inicial única.
Las bases existentes no reciben actualizaciones automáticas ni recuperan
productos eliminados; pueden editar estas descripciones manualmente en /admin.

Verificado el 2026-10-11: clean test y package aprobaron 223 pruebas cada uno;
Node aprobó 38, sin fallidas, errores ni omitidas. Chromium mostró las dos
descripciones y verificó filtros comida/bebidas y login/panel con SQLite nuevo
temporal. Ediciones y borrados sobrevivieron a reinicios en las pruebas;
todos los archivos de data/ conservaron su SHA-256. Sin cambios de interfaz,
esquema, autenticación ni dependencias. Solo commit local, sin push.

## Inicio automático y demostración local

Ampliación autorizada posterior a 92bbe90: perfil demo exclusivo, admin/admin
con BCrypt en memoria y validaciones de arranque para impedir escucha fuera de
127.0.0.1, mezcla de perfiles y selección de una base distinta de menu-demo.db.
Dev/prod conservan credenciales obligatorias; no se cambia la cadena de seguridad.

Lanzadores Bash y Windows con JDK 21 y Maven Wrapper, comprobación de puerto,
rutas con espacios y mensajes de inicio. Windows arranca el wrapper oficial
mediante Java para no depender de PowerShell; no cambian sus versiones ni las
dependencias de la aplicación. SQLite demo es independiente, persistente y usa
el esquema, migración y semillas existentes. No se cambian APIs ni interfaz.
El inicio rápido demo precede a las instrucciones avanzadas anteriores.

## Fuera del alcance

- Carrito, pagos, pedidos e inventario.
- Registro público de usuarios y funcionalidades no autorizadas.
- JPA/Hibernate, React, Angular, Docker y microservicios.

## Versionado y entrega

La entrega queda preparada para revisión pública del código y ejecución local,
con commits ordenados y README de inicio rápido. GitHub ya está configurado con el remoto `origin` hacia
`https://github.com/xpatto/el-buen-sabor.git`. En esta etapa se realiza un commit
local después de ejecutar y comunicar las pruebas. Cualquier push o cambio de
visibilidad requiere autorización explícita; no se realizan cambios remotos.

Cierre de etapa 4 (2026-10-09): `./mvnw clean test` y `./mvnw package`
terminaron con BUILD SUCCESS; cada comando ejecutó 113 pruebas, con 0 fallidas,
0 errores y 0 omitidas. Tomcat real verificó redirecciones al puerto efectivo
443, proxy, CSRF/login y cookies de producción; Boot rechazó el arranque con
credenciales ausentes o inválidas. Detalles y límites en docs/SEGURIDAD.md.

Cierre de etapa 5 (2026-10-09): clean test y package ejecutaron 179 pruebas
cada uno; 179 aprobadas, 0 fallidas, 0 errores y 0 omitidas. Datos de desarrollo
intactos por comparación SHA-256. Contrato y evidencia en API-ADMINISTRATIVA.md.

Cierre de etapa 6 (2026-10-11): clean test y package aprobaron 191 pruebas
cada uno, sin fallidas, errores ni omitidas; 34 pruebas JavaScript aprobadas
por separado. En la sesión de implementación pasaron 14 escenarios Chromium.
La lista manual Firefox está preparada y pendiente de ejecución. Datos de
desarrollo intactos por SHA-256. Detalles y límites en PRUEBAS-FRONTEND.md.


Cierre de etapa 6.1 (2026-10-11): ./mvnw clean test y ./mvnw package
finalizaron con BUILD SUCCESS, 204 pruebas cada uno, 0 fallidas, 0 errores y
0 omitidas. Node ejecutó 38 pruebas: 38 aprobadas, 0 fallidas y 0 omitidas.
La base de desarrollo y los demás archivos de data/ conservaron su SHA-256.
La lista manual de navegador de esta ampliación sigue pendiente; no se declara
como ejecutada. Detalles en docs/PRUEBAS-FRONTEND.md.

Cierre del modo demo (2026-10-11): `./mvnw clean test` y `./mvnw package`
aprobaron 245 pruebas Java cada uno; `node --test src/test/js/*.test.mjs`
aprobó 38. Cero fallidas, errores u omitidas. Las nuevas pruebas incluyen
Tomcat real, credenciales demo, CSRF, operaciones, reinicio, separación de bases,
rechazos de configuración y verificación de integridad del wrapper.

El lanzador Linux se ejecutó en una copia temporal con espacios, desde Bash y
Fish, con red local aislada para respetar el servicio existente en 8080. También
pasó el caso ZIP sin permisos ejecutables y la detección de puerto ocupado.
Chromium verificó siete tarjetas, variaciones, filtros, ausencia de enlace
administrativo y login/panel. Ctrl+C cerró Tomcat y SQLite; el reinicio conservó
una edición y una eliminación. Los archivos originales de data/ conservaron
su SHA-256. Windows recibió revisión estática de sintaxis, comillas y flujo;
el bootstrap Java descargó/verificó el wrapper y compiló en Linux. **No se ha
ejecutado el lanzador en Windows 10 real.** Sin push, despliegue ni etapas nuevas.

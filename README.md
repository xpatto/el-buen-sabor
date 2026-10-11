# El Buen Sabor

Menú digital del restaurante El Buen Sabor. Etapa 6.1: menú y panel administrativo
con eliminación definitiva y carga inicial persistente. Se conserva la API pública.
Dos páginas: menú en / y administración con inicio de sesión en /admin.
El menú público no muestra un enlace administrativo; se accede escribiendo
directamente `/admin` en la URL. La autenticación y las operaciones se conservan.

## Inicio rápido

Para uso local. Necesitas **Git, JDK 21** (incluye `java` y `javac`) e Internet
para la primera descarga de Maven y dependencias. No necesitas Maven instalado,
Docker ni un servidor de base de datos. Node solo es necesario para las pruebas
JavaScript. Ejecuta todos los comandos desde una terminal de tu equipo.

### Linux con Fish

```fish
git clone https://github.com/xpatto/el-buen-sabor.git
cd el-buen-sabor
java -version
javac -version
```

Ambos comandos Java deben indicar **21**. Si no, selecciona el JDK 21 antes de
continuar (incluido `JAVA_HOME`, si lo usas). Si GitHub pide acceso, necesitarás
permiso del propietario mientras el repositorio sea privado. Los cambios del
cierre son locales hasta que el propietario autorice y realice su publicación.

Prepara las bibliotecas del generador local y configura tus propias credenciales:

```fish
./mvnw -q dependency:build-classpath -DincludeScope=runtime -Dmdep.outputFile=target/local-classpath.txt
set -l config_local (mktemp -d)
java --class-path (cat target/local-classpath.txt) scripts/GenerarHash.java "$config_local/password.bcrypt"
```

La utilidad pide una contraseña de al menos 12 caracteres y su confirmación sin
mostrarlas. BCrypt admite hasta 72 bytes UTF-8. El hash usa coste 12 y se escribe
con permisos privados; no se imprime. **Continúa solo si terminó correctamente.**
No uses generadores web ni escribas contraseñas o hashes dentro de comandos.

```fish
read --prompt-str 'Usuario administrativo local: ' ADMIN_USERNAME
set -gx ADMIN_USERNAME $ADMIN_USERNAME
set -gx ADMIN_PASSWORD_HASH (cat "$config_local/password.bcrypt")
rm -- "$config_local/password.bcrypt"
rmdir -- "$config_local"
set -gx SPRING_PROFILES_ACTIVE dev
./mvnw spring-boot:run
```

El usuario no debe estar vacío ni tener espacios al principio o al final. Las
variables duran esta sesión de terminal; para otra sesión repite la configuración
(puedes usar la misma contraseña). No hay credenciales predeterminadas.
No compartas el entorno ni actives trazas de shell mientras contiene secretos.

- Menú público: <http://localhost:8080/>.
- Administración: <http://localhost:8080/admin>, escribiendo la URL directamente.
  El menú público no muestra un enlace administrativo. Inicia sesión con el
  usuario y la contraseña que acabas de elegir, **no con el hash**.
- Para detener: `Ctrl+C` en la terminal del servidor. Después limpia el entorno:

```fish
set -e ADMIN_USERNAME ADMIN_PASSWORD_HASH SPRING_PROFILES_ACTIVE
```

SQLite crea `data/menu.db` automáticamente al primer arranque, con tres
categorías y siete productos de demostración. No requiere instalar un servidor
SQL. Reiniciar conserva las ediciones y eliminaciones; no regenerará productos
borrados. El perfil local escucha solamente en `127.0.0.1`; no lo expongas a Internet.

Alternativas completas para [Bash y Windows PowerShell](docs/EJECUCION-LOCAL.md),
solución de problemas y comprobación desde una copia aislada en ese documento.

### Uso básico

En el menú selecciona Todos o una categoría: Comida / Lonchería, Cafetería y
Bebidas, o Tienda y Snacks. En `/admin` puedes agregar productos, editar sus
textos, precio y categoría, ocultarlos y reactivarlos. **Ocultar es reversible;
Eliminar borra definitivamente tras confirmar.** Actualiza el menú público para
ver los cambios. Las categorías no se administran desde el panel. Cierra sesión
al terminar; login, logout y escrituras mantienen protección CSRF.

### Pruebas y catálogo temporal

Desde la raíz, sin necesitar credenciales reales:

```fish
./mvnw clean test
./mvnw package
node --test src/test/js/*.test.mjs
```

Las pruebas usan bases aisladas. `package` repite las pruebas Java; Node se
ejecuta por separado. No se requieren `npm install` ni paquetes JavaScript.
Para probar manualmente desde cero, detén cualquier servidor anterior y, con
las credenciales locales configuradas, inicia con otra base:

```fish
set -l prueba_local (mktemp -d)
env APP_DB_PATH="$prueba_local/menu.db" ./mvnw spring-boot:run
```

Esa ejecución empieza con los datos de demostración y no altera `data/menu.db`.
Tras `Ctrl+C`, conserva o elimina únicamente el directorio temporal que creaste.
Volver a ejecutar sin esa variable recupera la base habitual. No borres tu base
real para reiniciar una prueba.

## Tecnologías

- Java 21 (JDK requerido).
- Spring Boot 3.5.16.
- Maven 3.9.11 mediante Maven Wrapper 3.3.4; no requiere Maven instalado.
- Spring Web, Spring Security, Thymeleaf, Validation y Spring Boot Starter Test.
- Spring Security Test, exclusivamente para pruebas.
- Frontend con HTML5, CSS Grid/Flexbox y módulos JavaScript nativos, sin frameworks,
  fuentes externas ni dependencias adicionales.
- Spring JDBC mediante `spring-boot-starter-jdbc` 3.5.16 (Spring JDBC 6.2.19,
  HikariCP 6.3.3) y SQLite JDBC de Xerial 3.49.1.0, en alcance runtime.
- Versiones administradas por el parent de Spring Boot; no se utiliza ORM.
- Coordenadas: `com.elbuensabor:menu`; paquete: `com.elbuensabor.menu`.
- Monolito por capas a implementar progresivamente.

## Pruebas y empaquetado

Para ejecutar el JAR conserva las variables administrativas del inicio rápido.

```fish
./mvnw clean test
./mvnw package
java -jar target/menu-0.0.1-SNAPSHOT.jar
```

Las pruebas usan SQLite real: una base nueva por ejecución bajo
`target/test-databases/` y un directorio temporal JUnit para probar reinicios.
Las clases con `@SpringBootTest` heredan de `SqliteIntegrationTest`, que impone
una URL dinámica con prioridad sobre `APP_DB_PATH`. Las pruebas de reinicio
fijan su propia ruta temporal antes de abrir el contexto. Ninguna prueba usa
`data/menu.db`; las operaciones de prueba sobre la base compartida se revierten
con transacciones. `clean` elimina las bases bajo `target/`, no las de desarrollo.

Se verifica arranque, conexión y archivo utilizado, semillas, precios exactos,
filtros parametrizados, ocultación/reactivación, restricciones SQL, claves
foráneas en todas las conexiones del pool e idempotencia conservando ediciones.
Las pruebas unitarias de servicios comprueban validación y conversión exacta de
centavos a `BigDecimal`. MockMvc verifica JSON, códigos HTTP, los filtros,
categorías sin activos y errores controlados; la integración usa los mismos
repositorios y SQLite reales. Los fallos simulados del test de errores pueden
producir trazas en el log de pruebas: no se incluyen en las respuestas HTTP.
Los reportes se generan en `target/surefire-reports/`.

## API REST pública

Con la aplicación iniciada mediante `./mvnw spring-boot:run` o el JAR, consulta:

| Solicitud | Respuesta |
| --- | --- |
| `GET /api/categorias` | Categorías con `id`, `nombre` y `slug` |
| `GET /api/productos` | Todos los productos activos |
| `GET /api/productos?categoria=comida` | Activos de la categoría indicada |

Ejemplos en Fish:

```fish
curl -i http://localhost:8080/api/categorias
curl -i http://localhost:8080/api/productos
curl -i 'http://localhost:8080/api/productos?categoria=comida'
curl -i 'http://localhost:8080/api/productos?categoria=bebidas'
curl -i 'http://localhost:8080/api/productos?categoria=tienda'
```

Respuesta inicial de categorías (`200 OK`):

```json
[
  {"id": 1, "nombre": "Comida / Lonchería", "slug": "comida"},
  {"id": 2, "nombre": "Cafetería y Bebidas", "slug": "bebidas"},
  {"id": 3, "nombre": "Tienda y Snacks", "slug": "tienda"}
]
```

Respuesta inicial con `categoria=comida` (`200 OK`):

```json
[
  {
    "id": 1,
    "nombre": "Torta de jamón",
    "categoria": "Comida / Lonchería",
    "precio": 45.00,
    "descripcion": "Jamón con queso y verduras."
  },
  {
    "id": 2,
    "nombre": "Chilaquiles verdes",
    "categoria": "Comida / Lonchería",
    "precio": 75.00,
    "descripcion": "Totopos en salsa verde con crema y queso. Disponibles con o sin picante."
  }
]
```

`categoria` es opcional: omitirlo lista todos los activos, ordenados por ID.
Los slugs se consultan en SQLite; inicialmente son `comida`, `bebidas` y
`tienda`. La comparación es exacta, sin recortar espacios ni cambiar mayúsculas.
Una categoría existente sin productos activos devuelve `200 OK` con `[]`.
La propiedad `categoria` de cada producto es el nombre legible, no su ID o slug.
`precio` es un número JSON convertido de centavos con `BigDecimal` a escala 2,
sin cálculos con `double` o `float`. Los precios de ejemplo son ilustrativos.

Filtro vacío, desconocido o repetido: `400 Bad Request`. Por ejemplo:

```fish
curl -i 'http://localhost:8080/api/productos?categoria='
curl -i 'http://localhost:8080/api/productos?categoria=inexistente'
```

```json
{"codigo":"CATEGORIA_INVALIDA","mensaje":"La categoría debe ser un único slug existente y no estar vacía."}
```

Los errores inesperados devuelven `500 Internal Server Error`:

```json
{"codigo":"ERROR_INTERNO","mensaje":"No fue posible procesar la solicitud."}
```

`ApiExceptionHandler` centraliza los errores, sin publicar trazas, SQL, rutas
internas ni mensajes del driver. Conserva los códigos HTTP de Spring MVC,
cuando la solicitud supera los filtros de seguridad. Las escrituras públicas
están denegadas: 403 sin CSRF; con CSRF, 401 anónimo o 403 autenticado.
No hay endpoints públicos de escritura.

## API administrativa (etapas 5 y 6.1)

Requiere sesión con ROLE_ADMIN y CSRF válido para POST, PUT, PATCH y DELETE.
El contrato completo, ejemplos JSON y el flujo de autenticación están en
[docs/API-ADMINISTRATIVA.md](docs/API-ADMINISTRATIVA.md).

| Método y ruta | Operación |
| --- | --- |
| GET /api/admin/productos | Lista todos, incluidos inactivos, por ID |
| POST /api/admin/productos | Crea un producto activo; 201 y Location |
| PUT /api/admin/productos/{id} | Actualiza información, categoría y precio; conserva activo |
| PATCH /api/admin/productos/{id}/estado | Oculta o reactiva; solo cambia activo |
| DELETE /api/admin/productos/{id} | Elimina físicamente; 204 sin cuerpo |

Nombre de 1–120 caracteres y descripción de 1–2000, sin aceptar valores
formados solo por espacios. Precio positivo con hasta dos decimales,
convertido exactamente a centavos long; máximo 92233720368547758.07.
Categoría existente e identificadores positivos; activo exige un booleano JSON.
La ampliación 6.1 autoriza DELETE solo para productos administrativos.
Eliminar es irreversible; Ocultar/Reactivar sigue siendo reversible.
No hay eliminación de categorías ni gestión de usuarios.

## Base de datos

Al ejecutar la aplicación desde la raíz se crea `./data/menu.db` automáticamente
si no existe. `data/.gitkeep` conserva el directorio en el repositorio.
Spring ejecuta `schema.sql` y `data.sql` al arrancar; no hay un paso manual de SQL.
No se necesitan usuario ni contraseña. Hikari ejecuta `PRAGMA foreign_keys=ON`
en cada conexión física nueva, incluidas las que añade al pool posteriormente.

Para cambiar la ubicación con Fish (el directorio padre debe existir):

```fish
mkdir -p /ruta/local/datos
env APP_DB_PATH=/ruta/local/datos/menu.db ./mvnw spring-boot:run
```

`APP_DB_PATH` es la ruta del archivo, sin el prefijo `jdbc:sqlite:`. Las rutas
relativas se resuelven desde el directorio de ejecución. Utiliza una ruta
persistente fuera de `target/` para desarrollo; los archivos SQLite y auxiliares
no se versionan. Mantén la extensión `.db`, `.sqlite` o `.sqlite3` para que
apliquen las exclusiones de Git.

| Tabla | Campos y restricciones |
| --- | --- |
| `categorias` | `id INTEGER PRIMARY KEY`, `nombre TEXT NOT NULL`, `slug TEXT NOT NULL UNIQUE` |
| `productos` | `id INTEGER PRIMARY KEY`, `nombre` y `descripcion TEXT NOT NULL`, `precio_centavos INTEGER NOT NULL` entero positivo, `categoria_id INTEGER NOT NULL` con FK a categorías, `activo INTEGER NOT NULL DEFAULT 1` limitado a 0/1 |

Los modelos `Categoria` y `Producto` son records en `domain`; identificadores y
centavos usan `long`, el estado usa `boolean`. `CategoriaRepository.listarTodas()`,
`ProductoRepository.listarActivos()` y `listarActivosPorCategoria(String slug)`
usan `JdbcTemplate` y `RowMapper`, ordenan por ID y parametrizan el slug.
ProductoRepository también lista todos, busca por ID, inserta con SQLite
INTEGER PRIMARY KEY y RETURNING, edita información, cambia el estado y elimina.
Los IDs se asignan mediante un contador persistente en la transacción del alta
para no reutilizar los de productos eliminados desde esta migración.
ProductoService agrupa las escrituras y lecturas de respuesta en transacciones.
La etapa 6.1 añade inicializacion_catalogo y adapta las semillas a una carga única.

Datos iniciales (precios ilustrativos, no información comercial real):

| Categoría (slug) | Productos y precios |
| --- | --- |
| Comida / Lonchería (`comida`) | Torta de jamón $45.00; Chilaquiles verdes $75.00 |
| Cafetería y Bebidas (`bebidas`) | Café americano $30.00; Licuado de fresa $40.00; Refresco $25.00 |
| Tienda y Snacks (`tienda`) | Galletas $18.00; Papitas $22.00 |

Cada producto incluye una descripción breve. Los precios se guardan en centavos
(por ejemplo, $45.00 = 4500), sin coma flotante.

### Conservación al reiniciar

Una base nueva recibe tres categorías y siete productos de demostración.
inicializacion_catalogo registra la carga completada y el último ID asignado.
Antes de crear categorias/productos, schema.sql detecta tablas anteriores:
si alguna existe, adopta el catálogo sin rellenar huecos ni sobrescribir datos,
incluso si está vacío. Para bases nuevas registra un estado pendiente;
data.sql inserta semillas y confirma la marca dentro de una transacción SAVEPOINT.
Una interrupción antes de completarla permite reintentar la carga.

Eliminar una semilla o todos los productos ya no los regenera al reiniciar.
Las bases anteriores se adoptan sin DROP TABLE ni reconstrucción. Los casos
históricos ambiguos y límites se documentan en
[API-ADMINISTRATIVA.md](docs/API-ADMINISTRATIVA.md).

## Alcance y colaboración

Consulta [docs/ALCANCE.md](docs/ALCANCE.md) y [AGENTS.md](AGENTS.md) antes de
cambiar el proyecto. La autenticación, las APIs y ambas interfaces están
implementadas dentro de las seis etapas y la ampliación 6.1. No hay credenciales de producción
ni bases de datos en el repositorio.

## Seguridad y autenticación

`GET /admin` muestra el formulario de acceso o el panel de productos autenticado.
Login (`POST /admin/login`) y logout (`POST /admin/logout`) requieren CSRF.
`/api/admin/**` requiere ADMIN: 401 JSON para anónimos, 403 sin rol y 404
para rutas todavía no implementadas cuando se supera la autorización.

El perfil predeterminado `dev` escucha en 127.0.0.1 por HTTP. Producción
requiere `SPRING_PROFILES_ACTIVE=prod`, credenciales externas, HTTPS y proxy
confiable configurado explícitamente. Las cookies llevan Secure en producción,
HttpOnly y SameSite=Lax, con sesión de 30 minutos. Configuración, resultados
y riesgos pendientes: [docs/SEGURIDAD.md](docs/SEGURIDAD.md).

## GitHub

GitHub ya está configurado: el remoto `origin` apunta a
[xpatto/el-buen-sabor](https://github.com/xpatto/el-buen-sabor).
Los commits de esta etapa son locales. Todo push o cambio remoto requiere
autorización explícita. La publicación o cambio de visibilidad queda a decisión del propietario.

Cierre de etapa 4 (2026-10-09): `./mvnw clean test` y `./mvnw package`
terminaron con BUILD SUCCESS; cada comando ejecutó 113 pruebas, con 0 fallidas,
0 errores y 0 omitidas. Tomcat real verificó redirecciones al puerto efectivo
443, proxy, CSRF/login y cookies de producción; Boot rechazó el arranque con
credenciales ausentes o inválidas. Detalles y límites en docs/SEGURIDAD.md.

Cierre de etapa 5 (2026-10-09): ./mvnw clean test y ./mvnw package terminaron
con BUILD SUCCESS. Cada comando ejecutó 179 pruebas: 179 aprobadas,
0 fallidas, 0 errores y 0 omitidas. Datos de desarrollo sin modificaciones,
verificados mediante SHA-256. Resultados, contrato y límites en
[API-ADMINISTRATIVA.md](docs/API-ADMINISTRATIVA.md).

## Interfaces web (etapa 6)

Abre http://localhost:8080/ para consultar el menú sin autenticarte. Todos es
la categoría inicial; el selector consulta la API por slug sin recargar la página.
Las tarjetas muestran nombre, categoría, descripción y precio exacto en MXN.
Se presentan estados de carga, catálogo vacío, error y reintento.

Abre http://localhost:8080/admin e inicia sesión para listar todos los productos,
agregar, editar, ocultar/reactivar o eliminar. El formulario reutiliza los datos del listado;
PUT conserva la disponibilidad y PATCH la modifica por separado. Cerrar sesión
continúa usando el formulario Thymeleaf protegido por CSRF. Una sesión vencida
muestra un enlace de autenticación y bloquea las escrituras.

Eliminar pide confirmación con el nombre y envía DELETE con CSRF. Cancelar no
envía ninguna petición; tras error de red se debe comprobar el listado antes
de repetir. Recuperar un producto eliminado exige restaurarlo desde un respaldo.

Los precios se mantienen como texto decimal exacto desde la lectura de JSON
hasta su envío; no se convierten a Number. Se admiten los límites del backend,
incluido 92233720368547758.07, sin redondeos. La interfaz exige punto decimal,
no coma ni notación exponencial. El servidor conserva sus validaciones.

Los archivos app.js y admin.js separan ambas interfaces; catalog.js comparte
la lectura exacta de JSON, validación de importes y solicitudes fetch. CSS y
JavaScript se sirven localmente. El token AJAX proviene de una metaetiqueta
Thymeleaf autenticada y nunca se guarda en localStorage.

Consulta [PRUEBAS-FRONTEND.md](docs/PRUEBAS-FRONTEND.md) para configurar
credenciales locales, ejecutar pruebas y realizar la lista manual de Firefox
en CachyOS. Las pruebas JavaScript usan el runner integrado de Node, sin npm
ni instalación de paquetes; Node no es necesario para ejecutar el JAR.

Cierre de etapa 6 (2026-10-11): clean test y package aprobaron 191 pruebas
cada uno, sin fallidas, errores ni omitidas; 34 pruebas JavaScript aprobadas
por separado. En la sesión de implementación pasaron 14 escenarios Chromium.
La lista manual Firefox está preparada y pendiente de ejecución. Datos de
desarrollo intactos por SHA-256. Detalles y límites en [PRUEBAS-FRONTEND.md](docs/PRUEBAS-FRONTEND.md).


Cierre de etapa 6.1 (2026-10-11): ./mvnw clean test y ./mvnw package
finalizaron con BUILD SUCCESS, 204 pruebas cada uno, 0 fallidas, 0 errores y
0 omitidas. Node ejecutó 38 pruebas: 38 aprobadas, 0 fallidas y 0 omitidas.
La base de desarrollo y los demás archivos de data/ conservaron su SHA-256.
La lista manual de navegador de esta ampliación sigue pendiente; no se declara
como ejecutada. Detalles en docs/PRUEBAS-FRONTEND.md.


Cierre local (2026-10-11): clean test y package aprobaron 219 pruebas cada uno;
Node aprobó 38, sin fallidas, errores ni omitidas. Inicio rápido verificado en
Fish y Bash desde una copia Git local aislada; Windows no ejecutado. Se revisaron
los ocho commits previos y los archivos nuevos sin secretos reales identificados.
Evidencia y límites en [EJECUCION-LOCAL.md](docs/EJECUCION-LOCAL.md).

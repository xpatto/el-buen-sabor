# El Buen Sabor

Menú digital del restaurante El Buen Sabor. Etapa actual: modelo de datos y
persistencia SQLite. Incluye modelos inmutables, repositorios de lectura y
pruebas de integración. La página temporal sigue mostrando «El Buen Sabor»
y «Proyecto inicializado».

## Tecnologías

- Java 21 (JDK requerido).
- Spring Boot 3.5.16.
- Maven 3.9.11 mediante Maven Wrapper 3.3.4; no requiere Maven instalado.
- Spring Web y Spring Boot Starter Test.
- Spring JDBC mediante `spring-boot-starter-jdbc` 3.5.16 (Spring JDBC 6.2.19,
  HikariCP 6.3.3) y SQLite JDBC de Xerial 3.49.1.0, en alcance runtime.
- Versiones administradas por el parent de Spring Boot; no se utiliza ORM.
- Coordenadas: `com.elbuensabor:menu`; paquete: `com.elbuensabor.menu`.
- Monolito por capas a implementar progresivamente.

## Ejecutar en Linux / Fish

Desde la raíz del proyecto, verifica el JDK:

```fish
java -version
javac -version
```

Ambos deben indicar Java 21. Si utilizas `JAVA_HOME`, debe apuntar al JDK 21.
La primera ejecución requiere Internet para descargar Maven y dependencias.
El Wrapper utiliza `curl` o `wget` y, según el descargador, `unzip`.

```fish
./mvnw spring-boot:run
```

Abre <http://localhost:8080>. Detén el servidor con `Ctrl+C`.
Si el checkout perdió el permiso de ejecución, ejecuta `chmod +x mvnw`.

## Pruebas y empaquetado

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
Los reportes se generan en `target/surefire-reports/`.

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
No hay operaciones de escritura en los repositorios.

Datos iniciales (precios ilustrativos, no información comercial real):

| Categoría (slug) | Productos y precios |
| --- | --- |
| Comida / Lonchería (`comida`) | Torta de jamón $45.00; Chilaquiles verdes $75.00 |
| Cafetería y Bebidas (`bebidas`) | Café americano $30.00; Licuado de fresa $40.00; Refresco $25.00 |
| Tienda y Snacks (`tienda`) | Galletas $18.00; Papitas $22.00 |

Cada producto incluye una descripción breve. Los precios se guardan en centavos
(por ejemplo, $45.00 = 4500), sin coma flotante.

### Conservación al reiniciar

Las tablas se crean solo si no existen. Las categorías usan IDs estables 1–3 y
los productos 1–7: las inserciones omiten conflictos, sin `UPDATE`, `REPLACE`
ni `DELETE`. Se preservan nombres, descripciones, precios, categorías asignadas,
estados y registros añadidos posteriormente. Un producto oculto permanece oculto.
Estos IDs se reservan para las semillas; no deben reutilizarse para otros datos.
El borrado físico no está contemplado: si se elimina una semilla manualmente,
puede volver a insertarse al arrancar. Los scripts no migran tablas existentes;
futuros cambios de estructura requerirán una migración explícita.

## Alcance y colaboración

Consulta [docs/ALCANCE.md](docs/ALCANCE.md) y [AGENTS.md](AGENTS.md) antes de
cambiar el proyecto. API REST, seguridad, administración e interfaz
definitiva están pendientes de etapas posteriores. No hay credenciales ni
bases de datos en el repositorio.

## GitHub

GitHub ya está configurado: el remoto `origin` apunta a
[xpatto/el-buen-sabor](https://github.com/xpatto/el-buen-sabor).
Los commits de esta etapa son locales. Todo push o cambio remoto requiere
autorización explícita. La entrega final requiere un repositorio público.

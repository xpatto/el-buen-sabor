# Alcance de El Buen Sabor

## Requisitos originales

- Menú digital sencillo de una o máximo dos páginas para el restaurante.
- Selector desplegable: Todos, Comida / Lonchería, Cafetería y Bebidas,
  Tienda y Snacks.
- Productos con nombre, categoría, precio base y descripción o variación.
- Interfaz limpia, legible y responsiva.
- API REST pública para obtener productos y categorías.

## Ampliaciones aprobadas

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
La página temporal de la etapa 1 se conserva.

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

## Etapa 4: seguridad y autenticación

- Spring Security y Thymeleaf; administrador en memoria con BCrypt y
  credenciales externas obligatorias, sin valores predeterminados.
- Formulario /admin, login/logout con CSRF, rotación del ID de sesión,
  errores genéricos y API pública de lectura conservada.
- /api/admin/** exige ADMIN; el resto no autorizado se deniega.
- Perfil dev local y prod con HTTPS público 443, proxy confiable explícito,
  cookies Secure/HttpOnly/SameSite=Lax y cabeceras de seguridad.
- Pruebas MockMvc y Tomcat real con SQLite aislado. Resultados de cierre
  en README.md y SEGURIDAD.md.
- Solo commit local tras verificaciones. Sin push, CRUD administrativo
  ni avance a etapa 5. El despliegue público requiere resolver los riesgos
  operativos documentados en SEGURIDAD.md.

## Fuera del alcance

- Carrito, pagos, pedidos e inventario.
- Registro público de usuarios y funcionalidades no autorizadas.
- JPA/Hibernate, React, Angular, Docker y microservicios.

## Versionado y entrega

La entrega final requiere GitHub público, múltiples commits ordenados y README
de ejecución. GitHub ya está configurado con el remoto `origin` hacia
`https://github.com/xpatto/el-buen-sabor.git`. En esta etapa se realiza un commit
local después de ejecutar y comunicar las pruebas. Cualquier push o cambio de
visibilidad requiere autorización explícita; no se realizan cambios remotos.

Cierre de etapa 4 (2026-10-09): `./mvnw clean test` y `./mvnw package`
terminaron con BUILD SUCCESS; cada comando ejecutó 113 pruebas, con 0 fallidas,
0 errores y 0 omitidas. Tomcat real verificó redirecciones al puerto efectivo
443, proxy, CSRF/login y cookies de producción; Boot rechazó el arranque con
credenciales ausentes o inválidas. Detalles y límites en docs/SEGURIDAD.md.

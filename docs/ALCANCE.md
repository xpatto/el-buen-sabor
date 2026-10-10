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

## Etapa actual: inicialización

Java 21, Spring Boot 3.5.16, Maven Wrapper, coordenadas `com.elbuensabor:menu`
y paquete `com.elbuensabor.menu`. Solo Spring Web y Spring Boot Starter Test.
Clase principal, prueba de contexto, página temporal, reglas de colaboración,
documentación y primer commit local en `main`.

La arquitectura futura será un monolito por capas, separando controladores,
servicios y repositorios conforme se implementen. No se crean capas vacías.
Esta etapa no implementa persistencia, endpoints, seguridad, administración
ni la interfaz definitiva.

## Fuera del alcance

- Carrito, pagos, pedidos e inventario.
- Registro público de usuarios y funcionalidades no autorizadas.
- JPA/Hibernate, React, Angular, Docker y microservicios.

## Versionado y entrega

La entrega final requiere GitHub público, múltiples commits ordenados y README
de ejecución. En esta etapa se preparará un repositorio **privado** llamado
`el-buen-sabor`; su creación y cualquier push requieren autorización explícita.
El cambio posterior a público también requiere autorización. No se realizan
cambios remotos durante la inicialización local.

# Plan de implementación: API REST pública

Especificación: solicitud de etapa 3 y [ALCANCE.md](ALCANCE.md).
Trabajo en la carpeta actual, sin modificar esquema, repositorios ni dependencias.

## Diseño y contrato

- `service/CategoriaService`: lista categorías mediante el repositorio existente.
- `service/ProductoService`: consulta categorías desde SQLite, valida el slug,
  obtiene productos activos y compone respuestas con nombre de categoría.
  Una transacción de lectura mantiene consistentes ambas consultas.
- `dto/CategoriaResponse` y `dto/ProductoResponse`: records de salida;
  `precio` usa `BigDecimal.valueOf(centavos, 2)`.
- `controller/CategoriaController` y `controller/ProductoController`: GET en
  `/api/categorias` y `/api/productos`, con `categoria` opcional.
- `exception/CategoriaInvalidaException` y `exception/ApiExceptionHandler`:
  error 400 para slug vacío o inexistente y 500 controlado para fallos
  inesperados. `dto/ApiError` contiene solo código y mensaje público.
  Se mantienen los códigos HTTP propios de errores de Spring MVC.

El slug se compara exactamente como está almacenado; no se normalizan espacios
ni mayúsculas. Filtro ausente devuelve todos los activos. Categoría existente
sin activos devuelve 200 con `[]`. Los parámetros repetidos se rechazan con 400.
No se exponen IDs internos de categoría, estado activo ni precios en centavos.

## Pasos

- [x] Escribir pruebas unitarias de servicios: DTO, precios exactos (incluidos
  centavos y valores grandes), validación desde datos, filtro y lista vacía.
- [x] Implementar servicios y DTO sobre los repositorios existentes.
- [x] Escribir pruebas MockMvc con SQLite aislado para los contratos HTTP y
  pruebas de errores con servicios simulados, sin acceder a desarrollo.
- [x] Implementar controladores y manejo central de excepciones.
- [x] Actualizar documentación, revisar y ejecutar `./mvnw clean test` y
  `./mvnw package`; comunicar resultados y crear commit local sin push.

## Riesgos que deben cubrir las pruebas

Parámetros vacíos, blancos, repetidos y con contenido SQL; categorías sin
activos; nombres legibles y campos JSON exactos; conversión decimal sin pérdida;
errores JDBC con texto sensible; métodos de escritura no permitidos. Conservar
las pruebas de las etapas anteriores y su aislamiento de SQLite.

## Verificación y correcciones

`./mvnw clean test` y `./mvnw package`: BUILD SUCCESS con 71 pruebas cada uno,
sin fallos, errores ni omisiones (33 anteriores y 38 nuevas). Revisión
independiente sin hallazgos que requieran corrección.

Se corrigió la firma utilizada en las aserciones JsonPath con BigDecimal.
Una prueba HTTP detectó que la conversión automática de Spring a `List<String>`
recortaba espacios del slug; el controlador ahora obtiene los valores originales
con `MultiValueMap` y rechaza los filtros inválidos sin normalizarlos.

Las excepciones JDBC simuladas se registran solo en logs y devuelven el JSON
genérico esperado. Continúan las advertencias conocidas de Mockito sobre carga
dinámica de agentes en Java 21; no causan fallos de pruebas. Sin push.

# API administrativa — etapa 5

API JSON para un administrador autenticado. Conserva la API pública de lectura,
los modelos y el esquema SQLite. La etapa 6 incorpora el panel en /admin
sobre este mismo contrato. No hay DELETE.

## Sesión y CSRF

1. GET /admin: guardar la cookie de sesión y extraer el input oculto `_csrf`.
2. POST /admin/login con formulario URL-encoded: username, password y ese
   `_csrf`, enviando la cookie. Las credenciales reales se configuran fuera de Git.
3. Después del 302 a /admin, conservar la nueva cookie (el ID de sesión rota)
   y obtener de nuevo GET /admin. Extraer el nuevo `_csrf` del formulario de logout.
4. Enviar la cookie autenticada en todas las operaciones administrativas.
   Para POST/PUT/PATCH JSON usar Content-Type: application/json y el encabezado
   X-CSRF-TOKEN con el token obtenido después del login.
5. Cerrar sesión con POST /admin/logout, cookie y token CSRF vigente.

No se usa HTTP Basic ni tokens bearer. Producción exige HTTPS y cookies Secure;
no reutilizar cookies/tokens entre sesiones ni publicar sus valores.
Ver [SEGURIDAD.md](SEGURIDAD.md) para proxy, credenciales y riesgos de publicación.

## Operaciones

### GET /api/admin/productos

200 OK con un arreglo de todos los productos, activos e inactivos, ordenado por
id ascendente. Sin filtros administrativos en esta etapa. Ejemplo de elemento:

```json
{
  "id": 8,
  "nombre": "Especial",
  "descripcion": "Con queso",
  "precio": 12.34,
  "categoriaId": 1,
  "categoria": "Comida / Lonchería",
  "activo": true
}
```

El precio se serializa como número decimal exacto con escala 2. Un consumidor
debe usar representación decimal adecuada para conservar la precisión,
especialmente cerca del máximo admitido.

### POST /api/admin/productos

```json
{"nombre":"Especial","descripcion":"Con queso","precio":12.34,"categoriaId":1}
```

201 Created, cuerpo con el producto completo y Location: /api/admin/productos/8
para el ID asignado. Location identifica el recurso editable mediante PUT;
no se incorpora un GET individual adicional. SQLite asigna el ID; se obtiene
con RETURNING en el mismo INSERT. El nuevo producto siempre está activo.

### PUT /api/admin/productos/{id}

```json
{"nombre":"Especial actualizado","descripcion":"Con extra de queso","precio":15.01,"categoriaId":2}
```

Reemplaza los cuatro campos editables de un producto existente. Todos son
obligatorios. Devuelve 200 OK con el producto completo. Conserva activo,
incluido cuando se edita un producto oculto.

### PATCH /api/admin/productos/{id}/estado

```json
{"activo":false}
```

false oculta; true reactiva. 200 OK con el producto completo. No cambia nombre,
descripción, categoría ni precio. Repetir el mismo estado también devuelve 200.
Los productos ocultos desaparecen de GET /api/productos y sus filtros;
reactivarlos los vuelve a incluir.

Solo los campos definidos en cada DTO se aplican. Los campos adicionales se
ignoran: activo en POST/PUT no permite alterar disponibilidad, y precio en
PATCH no permite editar el precio.

## Validación y errores

- nombre: obligatorio, no vacío ni solo espacios, máximo 120 caracteres.
- descripcion: obligatoria, no vacía ni solo espacios, máximo 2000 caracteres.
- No se recortan los textos válidos; se conservan tal como se reciben.
- precio: obligatorio, positivo, máximo dos decimales, máximo
  92233720368547758.07 (Long.MAX_VALUE centavos). No se redondea.
  Se rechaza también 1.000, aunque sus decimales adicionales sean ceros.
- categoriaId: entero JSON positivo dentro del rango long y categoría existente.
  No se truncan fracciones ni se aceptan cadenas como "1".
- IDs de ruta: long positivo. Formato incorrecto, cero, negativos o
  desbordamientos devuelven 400.
- activo: obligatorio, true o false JSON. Se rechazan null, cadenas, números,
  objetos y arreglos.
- JSON inválido y campos ausentes obligatorios devuelven 400.

| Estado | Significado |
| --- | --- |
| 200 | Listado, actualización o cambio de estado correcto |
| 201 | Producto creado |
| 400 | Datos, JSON, categoría o identificador inválidos |
| 401 | Sin autenticación, cuando la solicitud supera la comprobación CSRF |
| 403 | Rol insuficiente o CSRF ausente/inválido |
| 404 | Producto inexistente en PUT/PATCH con datos válidos |
| 405 | Método no implementado (por ejemplo DELETE), tras superar seguridad |
| 500 | Error inesperado; respuesta genérica |

Los errores mantienen el formato ApiError:

```json
{"codigo":"PRODUCTO_NO_ENCONTRADO","mensaje":"El producto no existe."}
```

Validación de JSON/DTO: SOLICITUD_INVALIDA; validación de precio/categoría/ID
en servicio: PRODUCTO_INVALIDO. La API pública conserva CATEGORIA_INVALIDA.
Errores de seguridad: NO_AUTENTICADO o ACCESO_DENEGADO.
Errores inesperados: ERROR_INTERNO, sin SQL, rutas internas ni detalles
de excepciones en la respuesta.

## Persistencia y límites

Las escrituras y la lectura del resultado pertenecen a una transacción
del servicio. Las consultas usan parámetros, sin concatenar datos del cliente.
No se modifica el esquema ni los scripts de inicialización; los cambios de
semillas y nuevos productos sobreviven al reinicio.

SQLite admite un escritor a la vez. Esta etapa no incorpora reintentos por
bloqueo ni control de versiones para ediciones simultáneas: un bloqueo puede
devolver un 500 controlado y la última edición confirmada prevalece.
No hay eliminación física, inventario ni gestión de categorías o usuarios.

Las pruebas cubren ciclo administrativo, límites de textos/precios, categoría
decimal rechazada, acceso con sesión/CSRF real, permisos, rollback conjunto y
persistencia a través de tres contextos. Cada base es temporal o está bajo
target; ninguna prueba utiliza data/menu.db.

## Verificación final de etapa 5 (2026-10-09)

| Comando | Ejecutadas | Aprobadas | Fallidas | Errores | Omitidas |
| --- | ---: | ---: | ---: | ---: | ---: |
| ./mvnw clean test | 179 | 179 | 0 | 0 | 0 |
| ./mvnw package | 179 | 179 | 0 | 0 | 0 |

Ambos terminaron con BUILD SUCCESS. Son 179 casos por ejecución (358
ejecuciones en los dos comandos), frente a los 113 de etapa 4. El recuento
se contrastó con los XML de target/surefire-reports. JAR generado sin clases
ni credenciales ficticias de pruebas.

La comparación SHA-256 de todos los archivos de data/ antes y después de
ambos comandos fue idéntica. data/menu.db no existía antes ni se creó durante
las pruebas. Las bases de integración y reinicio son aisladas.

La revisión detectó y corrigió la coerción de categoriaId decimal a entero:
la regresión recibió 201 antes del arreglo y ahora devuelve 400 sin mutación.
Se conservaron las pruebas de etapa 4, adaptando las expectativas de rutas
que ahora existen; la seguridad no fue desactivada ni modificada.
Revisión manual y búsqueda de patrones en los 65 archivos candidatos: sin
secretos reales identificados. No sustituye una auditoría de seguridad.

El commit de etapa 5 es exclusivamente local; no se realiza push, despliegue,
panel definitivo ni avance a etapa 6. Persisten los riesgos operativos de
SEGURIDAD.md y la limitación de concurrencia SQLite descrita en esta API.

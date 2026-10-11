-- Datos ilustrativos: los precios no representan información comercial real.
-- Carga única y atómica. SAVEPOINT funciona también dentro de una transacción.
-- completada=0 persiste si el primer arranque se interrumpe antes de esta carga.
SAVEPOINT carga_inicial_catalogo;
WITH semillas(id, nombre, slug) AS (VALUES
    (1, 'Comida / Lonchería', 'comida'),
    (2, 'Cafetería y Bebidas', 'bebidas'),
    (3, 'Tienda y Snacks', 'tienda'))
INSERT INTO categorias (id, nombre, slug)
SELECT id, nombre, slug FROM semillas
WHERE (SELECT completada FROM inicializacion_catalogo WHERE id=1) = 0
ON CONFLICT DO NOTHING;

INSERT INTO productos (id, nombre, descripcion, precio_centavos, categoria_id)
SELECT 1, 'Torta de jamón', 'Jamón con queso y verduras.', 4500, id FROM categorias WHERE slug = 'comida' AND (SELECT completada FROM inicializacion_catalogo WHERE id=1) = 0
ON CONFLICT (id) DO NOTHING;

INSERT INTO productos (id, nombre, descripcion, precio_centavos, categoria_id)
SELECT 2, 'Chilaquiles verdes', 'Totopos en salsa verde con crema y queso.', 7500, id FROM categorias WHERE slug = 'comida' AND (SELECT completada FROM inicializacion_catalogo WHERE id=1) = 0
ON CONFLICT (id) DO NOTHING;

INSERT INTO productos (id, nombre, descripcion, precio_centavos, categoria_id)
SELECT 3, 'Café americano', 'Café negro caliente.', 3000, id FROM categorias WHERE slug = 'bebidas' AND (SELECT completada FROM inicializacion_catalogo WHERE id=1) = 0
ON CONFLICT (id) DO NOTHING;

INSERT INTO productos (id, nombre, descripcion, precio_centavos, categoria_id)
SELECT 4, 'Licuado de fresa', 'Fresa licuada con leche.', 4000, id FROM categorias WHERE slug = 'bebidas' AND (SELECT completada FROM inicializacion_catalogo WHERE id=1) = 0
ON CONFLICT (id) DO NOTHING;

INSERT INTO productos (id, nombre, descripcion, precio_centavos, categoria_id)
SELECT 5, 'Refresco', 'Refresco individual, sabor a elegir.', 2500, id FROM categorias WHERE slug = 'bebidas' AND (SELECT completada FROM inicializacion_catalogo WHERE id=1) = 0
ON CONFLICT (id) DO NOTHING;

INSERT INTO productos (id, nombre, descripcion, precio_centavos, categoria_id)
SELECT 6, 'Galletas', 'Paquete individual de galletas.', 1800, id FROM categorias WHERE slug = 'tienda' AND (SELECT completada FROM inicializacion_catalogo WHERE id=1) = 0
ON CONFLICT (id) DO NOTHING;

INSERT INTO productos (id, nombre, descripcion, precio_centavos, categoria_id)
SELECT 7, 'Papitas', 'Bolsa individual de papas fritas.', 2200, id FROM categorias WHERE slug = 'tienda' AND (SELECT completada FROM inicializacion_catalogo WHERE id=1) = 0
ON CONFLICT (id) DO NOTHING;

UPDATE inicializacion_catalogo
SET completada=1,
    ultimo_producto_id=MAX(ultimo_producto_id, COALESCE((SELECT MAX(id) FROM productos), 0))
WHERE id=1 AND completada=0;
RELEASE SAVEPOINT carga_inicial_catalogo;

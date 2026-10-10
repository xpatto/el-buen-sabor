-- Datos ilustrativos: los precios no representan información comercial real.
-- IDs estables: los conflictos se omiten, nunca se reemplazan las ediciones.
INSERT INTO categorias (id, nombre, slug) VALUES
    (1, 'Comida / Lonchería', 'comida'),
    (2, 'Cafetería y Bebidas', 'bebidas'),
    (3, 'Tienda y Snacks', 'tienda')
ON CONFLICT DO NOTHING;

INSERT INTO productos (id, nombre, descripcion, precio_centavos, categoria_id)
SELECT 1, 'Torta de jamón', 'Jamón con queso y verduras.', 4500, id FROM categorias WHERE slug = 'comida'
ON CONFLICT (id) DO NOTHING;

INSERT INTO productos (id, nombre, descripcion, precio_centavos, categoria_id)
SELECT 2, 'Chilaquiles verdes', 'Totopos en salsa verde con crema y queso.', 7500, id FROM categorias WHERE slug = 'comida'
ON CONFLICT (id) DO NOTHING;

INSERT INTO productos (id, nombre, descripcion, precio_centavos, categoria_id)
SELECT 3, 'Café americano', 'Café negro caliente.', 3000, id FROM categorias WHERE slug = 'bebidas'
ON CONFLICT (id) DO NOTHING;

INSERT INTO productos (id, nombre, descripcion, precio_centavos, categoria_id)
SELECT 4, 'Licuado de fresa', 'Fresa licuada con leche.', 4000, id FROM categorias WHERE slug = 'bebidas'
ON CONFLICT (id) DO NOTHING;

INSERT INTO productos (id, nombre, descripcion, precio_centavos, categoria_id)
SELECT 5, 'Refresco', 'Refresco individual, sabor a elegir.', 2500, id FROM categorias WHERE slug = 'bebidas'
ON CONFLICT (id) DO NOTHING;

INSERT INTO productos (id, nombre, descripcion, precio_centavos, categoria_id)
SELECT 6, 'Galletas', 'Paquete individual de galletas.', 1800, id FROM categorias WHERE slug = 'tienda'
ON CONFLICT (id) DO NOTHING;

INSERT INTO productos (id, nombre, descripcion, precio_centavos, categoria_id)
SELECT 7, 'Papitas', 'Bolsa individual de papas fritas.', 2200, id FROM categorias WHERE slug = 'tienda'
ON CONFLICT (id) DO NOTHING;

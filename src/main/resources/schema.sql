-- Registrar el estado ANTES de crear las tablas del catálogo.
-- Una tabla antigua (incluso vacía) es evidencia de una base previa: no rellenar.
CREATE TABLE IF NOT EXISTS inicializacion_catalogo (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    completada INTEGER NOT NULL CHECK (completada IN (0, 1)),
    ultimo_producto_id INTEGER NOT NULL DEFAULT 0
        CHECK (typeof(ultimo_producto_id) = 'integer' AND ultimo_producto_id >= 0)
);

INSERT INTO inicializacion_catalogo (id, completada)
SELECT 1, CASE WHEN EXISTS (
    SELECT 1 FROM sqlite_master WHERE type = 'table' AND name IN ('categorias', 'productos')
) THEN 1 ELSE 0 END
WHERE NOT EXISTS (SELECT 1 FROM inicializacion_catalogo WHERE id = 1);

CREATE TABLE IF NOT EXISTS categorias (
    id INTEGER PRIMARY KEY,
    nombre TEXT NOT NULL,
    slug TEXT NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS productos (
    id INTEGER PRIMARY KEY,
    nombre TEXT NOT NULL,
    descripcion TEXT NOT NULL,
    precio_centavos INTEGER NOT NULL
        CHECK (typeof(precio_centavos) = 'integer' AND precio_centavos > 0),
    categoria_id INTEGER NOT NULL REFERENCES categorias(id),
    activo INTEGER NOT NULL DEFAULT 1 CHECK (activo IN (0, 1))
);

-- Adoptar el máximo existente al migrar sin reconstruir ni modificar productos.
UPDATE inicializacion_catalogo
SET ultimo_producto_id = MAX(ultimo_producto_id, COALESCE((SELECT MAX(id) FROM productos), 0))
WHERE id = 1;

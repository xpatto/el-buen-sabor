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

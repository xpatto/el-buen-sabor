package com.elbuensabor.menu.repository;

import java.util.List;
import java.util.Optional;

import com.elbuensabor.menu.domain.Producto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class ProductoRepository {

    private static final String SELECT_ACTIVE = """
            SELECT p.id, p.nombre, p.descripcion, p.precio_centavos, p.categoria_id, p.activo
            FROM productos p
            JOIN categorias c ON c.id = p.categoria_id
            WHERE p.activo = 1
            """;

    private static final RowMapper<Producto> ROW_MAPPER = (result, rowNumber) -> new Producto(
            result.getLong("id"),
            result.getString("nombre"),
            result.getString("descripcion"),
            result.getLong("precio_centavos"),
            result.getLong("categoria_id"),
            result.getInt("activo") == 1);

    private final JdbcTemplate jdbc;

    public ProductoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Producto> listarActivos() {
        return jdbc.query(SELECT_ACTIVE + " ORDER BY p.id", ROW_MAPPER);
    }

    public List<Producto> listarActivosPorCategoria(String slug) {
        return jdbc.query(SELECT_ACTIVE + " AND c.slug = ? ORDER BY p.id", ROW_MAPPER, slug);
    }

    public List<Producto> listarTodos() {
        return jdbc.query("SELECT id, nombre, descripcion, precio_centavos, categoria_id, activo FROM productos ORDER BY id",
                ROW_MAPPER);
    }

    public Optional<Producto> buscarPorId(long id) {
        return jdbc.query("SELECT id, nombre, descripcion, precio_centavos, categoria_id, activo FROM productos WHERE id = ?",
                ROW_MAPPER, id).stream().findFirst();
    }

    public Producto crear(String nombre, String descripcion, long precioCentavos, long categoriaId) {
        // La transacción del servicio asigna y consume el ID; nunca reutilizar uno borrado.
        Long id = jdbc.queryForObject("""
                UPDATE inicializacion_catalogo
                SET ultimo_producto_id = MAX(ultimo_producto_id, COALESCE((SELECT MAX(id) FROM productos), 0)) + 1
                WHERE id = 1
                  AND MAX(ultimo_producto_id, COALESCE((SELECT MAX(id) FROM productos), 0)) < 9223372036854775807
                RETURNING ultimo_producto_id
                """, Long.class);
        return jdbc.queryForObject("""
                INSERT INTO productos (id, nombre, descripcion, precio_centavos, categoria_id, activo)
                VALUES (?, ?, ?, ?, ?, 1)
                RETURNING id, nombre, descripcion, precio_centavos, categoria_id, activo
                """, ROW_MAPPER, id, nombre, descripcion, precioCentavos, categoriaId);
    }

    public int actualizar(long id, String nombre, String descripcion, long precioCentavos, long categoriaId) {
        return jdbc.update("""
                UPDATE productos SET nombre = ?, descripcion = ?, precio_centavos = ?, categoria_id = ? WHERE id = ?
                """, nombre, descripcion, precioCentavos, categoriaId, id);
    }

    public int cambiarEstado(long id, boolean activo) {
        return jdbc.update("UPDATE productos SET activo = ? WHERE id = ?", activo ? 1 : 0, id);
    }

    public int eliminar(long id) {
        return jdbc.update("DELETE FROM productos WHERE id = ?", id);
    }
}

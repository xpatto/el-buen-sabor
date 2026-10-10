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
        // SQLite asigna INTEGER PRIMARY KEY; RETURNING obtiene la fila del mismo INSERT.
        return jdbc.queryForObject("""
                INSERT INTO productos (nombre, descripcion, precio_centavos, categoria_id, activo)
                VALUES (?, ?, ?, ?, 1)
                RETURNING id, nombre, descripcion, precio_centavos, categoria_id, activo
                """, ROW_MAPPER, nombre, descripcion, precioCentavos, categoriaId);
    }

    public int actualizar(long id, String nombre, String descripcion, long precioCentavos, long categoriaId) {
        return jdbc.update("""
                UPDATE productos SET nombre = ?, descripcion = ?, precio_centavos = ?, categoria_id = ? WHERE id = ?
                """, nombre, descripcion, precioCentavos, categoriaId, id);
    }

    public int cambiarEstado(long id, boolean activo) {
        return jdbc.update("UPDATE productos SET activo = ? WHERE id = ?", activo ? 1 : 0, id);
    }
}

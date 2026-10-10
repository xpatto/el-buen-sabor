package com.elbuensabor.menu.repository;

import java.util.List;

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
}

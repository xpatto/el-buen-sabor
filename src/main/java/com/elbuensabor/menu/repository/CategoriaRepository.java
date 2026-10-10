package com.elbuensabor.menu.repository;

import java.util.List;

import com.elbuensabor.menu.domain.Categoria;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class CategoriaRepository {

    private static final RowMapper<Categoria> ROW_MAPPER = (result, rowNumber) -> new Categoria(
            result.getLong("id"), result.getString("nombre"), result.getString("slug"));

    private final JdbcTemplate jdbc;

    public CategoriaRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Categoria> listarTodas() {
        return jdbc.query("SELECT id, nombre, slug FROM categorias ORDER BY id", ROW_MAPPER);
    }
}

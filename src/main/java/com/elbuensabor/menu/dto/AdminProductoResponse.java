package com.elbuensabor.menu.dto;

import java.math.BigDecimal;

public record AdminProductoResponse(long id, String nombre, String descripcion,
        BigDecimal precio, long categoriaId, String categoria, boolean activo) {
}

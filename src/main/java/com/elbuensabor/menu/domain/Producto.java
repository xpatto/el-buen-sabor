package com.elbuensabor.menu.domain;

public record Producto(
        long id,
        String nombre,
        String descripcion,
        long precioCentavos,
        long categoriaId,
        boolean activo) {
}

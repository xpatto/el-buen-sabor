package com.elbuensabor.menu.dto;

import java.math.BigDecimal;

public record ProductoResponse(long id, String nombre, String categoria, BigDecimal precio, String descripcion) {
}

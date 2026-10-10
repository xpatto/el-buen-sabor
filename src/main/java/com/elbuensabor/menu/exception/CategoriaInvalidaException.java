package com.elbuensabor.menu.exception;

public class CategoriaInvalidaException extends RuntimeException {

    public CategoriaInvalidaException() {
        super("La categoría debe ser un único slug existente y no estar vacía.");
    }
}

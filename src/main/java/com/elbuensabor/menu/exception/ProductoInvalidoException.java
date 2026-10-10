package com.elbuensabor.menu.exception;

public class ProductoInvalidoException extends RuntimeException {
    public ProductoInvalidoException() {
        super("Los datos del producto no son válidos.");
    }
}

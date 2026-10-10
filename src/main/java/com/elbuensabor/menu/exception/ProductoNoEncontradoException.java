package com.elbuensabor.menu.exception;

public class ProductoNoEncontradoException extends RuntimeException {
    public ProductoNoEncontradoException() {
        super("El producto no existe.");
    }
}

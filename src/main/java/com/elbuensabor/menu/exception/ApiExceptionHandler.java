package com.elbuensabor.menu.exception;

import com.elbuensabor.menu.dto.ApiError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);
    private static final ApiError INTERNAL_ERROR = new ApiError("ERROR_INTERNO", "No fue posible procesar la solicitud.");

    @ExceptionHandler(ProductoInvalidoException.class)
    public ResponseEntity<ApiError> productoInvalido(ProductoInvalidoException exception) {
        return ResponseEntity.badRequest().body(new ApiError("PRODUCTO_INVALIDO", exception.getMessage()));
    }

    @ExceptionHandler(ProductoNoEncontradoException.class)
    public ResponseEntity<ApiError> productoNoEncontrado(ProductoNoEncontradoException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError("PRODUCTO_NO_ENCONTRADO", exception.getMessage()));
    }

    @ExceptionHandler(CategoriaInvalidaException.class)
    public ResponseEntity<ApiError> categoriaInvalida(CategoriaInvalidaException exception) {
        return ResponseEntity.badRequest().body(new ApiError("CATEGORIA_INVALIDA", exception.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> errorInesperado(Exception exception) {
        LOGGER.error("Error inesperado al procesar la solicitud", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(INTERNAL_ERROR);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception exception, Object body,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        // Conservar códigos como 404/405/406 sin publicar mensajes internos de Spring.
        ApiError error = status.is5xxServerError()
                ? INTERNAL_ERROR : new ApiError("SOLICITUD_INVALIDA", "La solicitud no es válida.");
        return super.handleExceptionInternal(exception, error, headers, status, request);
    }
}

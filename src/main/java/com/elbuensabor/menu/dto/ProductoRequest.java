package com.elbuensabor.menu.dto;

import java.math.BigDecimal;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Datos editables; el estado se modifica exclusivamente mediante PATCH. */
public record ProductoRequest(
        @NotBlank @Size(max = 120) String nombre,
        @NotBlank @Size(max = 2000) String descripcion,
        @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 17, fraction = 2) BigDecimal precio,
        @NotNull @Positive @JsonDeserialize(using = IdentificadorDeserializer.class) Long categoriaId) {

    /** Un identificador JSON debe ser entero; nunca truncar una categoría decimal. */
    public static class IdentificadorDeserializer extends JsonDeserializer<Long> {
        @Override
        public Long deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            if (parser.hasToken(JsonToken.VALUE_NUMBER_INT)) {
                return parser.getLongValue();
            }
            return (Long) context.handleUnexpectedToken(Long.class, parser);
        }
    }
}

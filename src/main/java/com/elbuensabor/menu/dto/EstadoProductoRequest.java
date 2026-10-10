package com.elbuensabor.menu.dto;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotNull;

public record EstadoProductoRequest(
        @NotNull @JsonDeserialize(using = BooleanEstrictoDeserializer.class) Boolean activo) {

    /** Impide convertir silenciosamente cadenas o números a booleanos. */
    public static class BooleanEstrictoDeserializer extends JsonDeserializer<Boolean> {
        @Override
        public Boolean deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            if (parser.hasToken(JsonToken.VALUE_TRUE)) {
                return true;
            }
            if (parser.hasToken(JsonToken.VALUE_FALSE)) {
                return false;
            }
            return (Boolean) context.handleUnexpectedToken(Boolean.class, parser);
        }
    }
}

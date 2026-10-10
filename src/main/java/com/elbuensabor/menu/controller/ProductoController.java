package com.elbuensabor.menu.controller;

import java.util.List;

import com.elbuensabor.menu.dto.ProductoResponse;
import com.elbuensabor.menu.exception.CategoriaInvalidaException;
import com.elbuensabor.menu.service.ProductoService;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoService service;

    public ProductoController(ProductoService service) {
        this.service = service;
    }

    @GetMapping
    public List<ProductoResponse> listar(@RequestParam MultiValueMap<String, String> parametros) {
        // Conservar los valores originales: la conversión a List<String> recorta espacios.
        List<String> categorias = parametros.get("categoria");
        if (categorias != null && categorias.size() != 1) {
            throw new CategoriaInvalidaException();
        }
        return service.listarActivos(categorias == null ? null : categorias.getFirst());
    }
}

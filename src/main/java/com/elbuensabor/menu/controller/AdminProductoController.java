package com.elbuensabor.menu.controller;

import java.net.URI;
import java.util.List;

import com.elbuensabor.menu.dto.AdminProductoResponse;
import com.elbuensabor.menu.dto.EstadoProductoRequest;
import com.elbuensabor.menu.dto.ProductoRequest;
import com.elbuensabor.menu.service.ProductoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/productos")
public class AdminProductoController {

    private final ProductoService service;

    public AdminProductoController(ProductoService service) {
        this.service = service;
    }

    @GetMapping
    public List<AdminProductoResponse> listar() {
        return service.listarTodos();
    }

    @PostMapping
    public ResponseEntity<AdminProductoResponse> crear(@Valid @RequestBody ProductoRequest request) {
        var producto = service.crear(request);
        return ResponseEntity.created(URI.create("/api/admin/productos/" + producto.id())).body(producto);
    }

    @PutMapping("/{id}")
    public AdminProductoResponse actualizar(@PathVariable long id, @Valid @RequestBody ProductoRequest request) {
        return service.actualizar(id, request);
    }

    @PatchMapping("/{id}/estado")
    public AdminProductoResponse cambiarEstado(@PathVariable long id, @Valid @RequestBody EstadoProductoRequest request) {
        return service.cambiarEstado(id, request.activo());
    }
}

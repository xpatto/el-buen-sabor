package com.elbuensabor.menu.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.elbuensabor.menu.domain.Categoria;
import com.elbuensabor.menu.dto.ProductoResponse;
import com.elbuensabor.menu.exception.CategoriaInvalidaException;
import com.elbuensabor.menu.repository.CategoriaRepository;
import com.elbuensabor.menu.repository.ProductoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductoService {

    private final ProductoRepository products;
    private final CategoriaRepository categories;

    public ProductoService(ProductoRepository products, CategoriaRepository categories) {
        this.products = products;
        this.categories = categories;
    }

    @Transactional(readOnly = true)
    public List<ProductoResponse> listarActivos(String slug) {
        if (slug != null && slug.isBlank()) {
            throw new CategoriaInvalidaException();
        }
        var categorias = categories.listarTodas();
        if (slug != null && categorias.stream().noneMatch(categoria -> categoria.slug().equals(slug))) {
            throw new CategoriaInvalidaException();
        }
        Map<Long, Categoria> porId = categorias.stream()
                .collect(Collectors.toMap(Categoria::id, Function.identity()));
        var productos = slug == null ? products.listarActivos() : products.listarActivosPorCategoria(slug);

        return productos.stream().map(producto -> new ProductoResponse(
                producto.id(), producto.nombre(), porId.get(producto.categoriaId()).nombre(),
                BigDecimal.valueOf(producto.precioCentavos(), 2), producto.descripcion())).toList();
    }
}

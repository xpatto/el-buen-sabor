package com.elbuensabor.menu.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.elbuensabor.menu.domain.Categoria;
import com.elbuensabor.menu.domain.Producto;
import com.elbuensabor.menu.dto.AdminProductoResponse;
import com.elbuensabor.menu.dto.ProductoRequest;
import com.elbuensabor.menu.dto.ProductoResponse;
import com.elbuensabor.menu.exception.CategoriaInvalidaException;
import com.elbuensabor.menu.exception.ProductoInvalidoException;
import com.elbuensabor.menu.exception.ProductoNoEncontradoException;
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

    @Transactional(readOnly = true)
    public List<AdminProductoResponse> listarTodos() {
        Map<Long, Categoria> porId = categories.listarTodas().stream()
                .collect(Collectors.toMap(Categoria::id, Function.identity()));
        return products.listarTodos().stream()
                .map(producto -> respuestaAdministrativa(producto, porId.get(producto.categoriaId()))).toList();
    }

    @Transactional
    public AdminProductoResponse crear(ProductoRequest request) {
        long centavos = precioEnCentavos(request.precio());
        Categoria categoria = categoriaExistente(request.categoriaId());
        Producto producto = products.crear(request.nombre(), request.descripcion(), centavos, categoria.id());
        return respuestaAdministrativa(producto, categoria);
    }

    @Transactional
    public AdminProductoResponse actualizar(long id, ProductoRequest request) {
        validarId(id);
        long centavos = precioEnCentavos(request.precio());
        Categoria categoria = categoriaExistente(request.categoriaId());
        if (products.actualizar(id, request.nombre(), request.descripcion(), centavos, categoria.id()) == 0) {
            throw new ProductoNoEncontradoException();
        }
        return respuestaAdministrativa(productoExistente(id), categoria);
    }

    @Transactional
    public AdminProductoResponse cambiarEstado(long id, boolean activo) {
        validarId(id);
        if (products.cambiarEstado(id, activo) == 0) {
            throw new ProductoNoEncontradoException();
        }
        Producto producto = productoExistente(id);
        return respuestaAdministrativa(producto, categoriaExistente(producto.categoriaId()));
    }

    @Transactional
    public void eliminar(long id) {
        validarId(id);
        if (products.eliminar(id) == 0) {
            throw new ProductoNoEncontradoException();
        }
    }

    private long precioEnCentavos(BigDecimal precio) {
        if (precio == null || precio.signum() <= 0 || precio.scale() > 2
                || precio.compareTo(BigDecimal.valueOf(Long.MAX_VALUE, 2)) > 0) {
            throw new ProductoInvalidoException();
        }
        try {
            return precio.movePointRight(2).longValueExact();
        } catch (ArithmeticException exception) {
            throw new ProductoInvalidoException();
        }
    }

    private void validarId(long id) {
        if (id <= 0) {
            throw new ProductoInvalidoException();
        }
    }

    private Categoria categoriaExistente(Long id) {
        if (id == null || id <= 0) {
            throw new ProductoInvalidoException();
        }
        return categories.listarTodas().stream().filter(categoria -> categoria.id() == id)
                .findFirst().orElseThrow(ProductoInvalidoException::new);
    }

    private Producto productoExistente(long id) {
        return products.buscarPorId(id).orElseThrow(ProductoNoEncontradoException::new);
    }

    private AdminProductoResponse respuestaAdministrativa(Producto producto, Categoria categoria) {
        return new AdminProductoResponse(producto.id(), producto.nombre(), producto.descripcion(),
                BigDecimal.valueOf(producto.precioCentavos(), 2), categoria.id(), categoria.nombre(), producto.activo());
    }
}

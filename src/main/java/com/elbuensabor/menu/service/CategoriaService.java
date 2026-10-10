package com.elbuensabor.menu.service;

import java.util.List;

import com.elbuensabor.menu.dto.CategoriaResponse;
import com.elbuensabor.menu.repository.CategoriaRepository;
import org.springframework.stereotype.Service;

@Service
public class CategoriaService {

    private final CategoriaRepository repository;

    public CategoriaService(CategoriaRepository repository) {
        this.repository = repository;
    }

    public List<CategoriaResponse> listarTodas() {
        return repository.listarTodas().stream()
                .map(categoria -> new CategoriaResponse(categoria.id(), categoria.nombre(), categoria.slug()))
                .toList();
    }
}

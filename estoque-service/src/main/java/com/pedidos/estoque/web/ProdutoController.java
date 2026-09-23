package com.pedidos.estoque.web;

import com.pedidos.estoque.service.ProdutoService;
import com.pedidos.estoque.web.ProdutoDtos.CriarProdutoRequest;
import com.pedidos.estoque.web.ProdutoDtos.ProdutoResponse;
import com.pedidos.estoque.web.ProdutoDtos.QuantidadeRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/produtos")
public class ProdutoController {

    private final ProdutoService service;

    public ProdutoController(ProdutoService service) {
        this.service = service;
    }

    @GetMapping
    public List<ProdutoResponse> listar() {
        return service.listar().stream().map(ProdutoResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ProdutoResponse detalhar(@PathVariable String id) {
        return ProdutoResponse.from(service.buscarPorId(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProdutoResponse criar(@Valid @RequestBody CriarProdutoRequest request) {
        return ProdutoResponse.from(service.criar(request.toEntity()));
    }

    @PostMapping("/{id}/reservar")
    public ProdutoResponse reservar(@PathVariable String id, @Valid @RequestBody QuantidadeRequest request) {
        return ProdutoResponse.from(service.reservar(id, request.quantidade()));
    }

    @PostMapping("/{id}/liberar")
    public ProdutoResponse liberar(@PathVariable String id, @Valid @RequestBody QuantidadeRequest request) {
        return ProdutoResponse.from(service.liberar(id, request.quantidade()));
    }

    @PostMapping("/{id}/confirmar")
    public ProdutoResponse confirmar(@PathVariable String id, @Valid @RequestBody QuantidadeRequest request) {
        return ProdutoResponse.from(service.confirmar(id, request.quantidade()));
    }
}

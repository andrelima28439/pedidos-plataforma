package com.pedidos.estoque.service;

import com.pedidos.estoque.domain.Produto;
import com.pedidos.estoque.repository.ProdutoRepository;
import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

@Service
public class ProdutoService {

    private final ProdutoRepository repository;

    public ProdutoService(ProdutoRepository repository) {
        this.repository = repository;
    }

    @Cacheable(value = "produtos")
    public List<Produto> listar() {
        return repository.findAll();
    }

    @Cacheable(value = "produto", key = "#id")
    public Produto buscarPorId(String id) {
        return repository.findById(id).orElseThrow(() -> new ProdutoNaoEncontradoException(id));
    }

    @CacheEvict(
            value = {"produtos", "produto"},
            allEntries = true)
    public Produto criar(Produto produto) {
        produto.setId(null);
        produto.setVersion(null);
        return repository.save(produto);
    }

    @CacheEvict(
            value = {"produtos", "produto"},
            allEntries = true)
    public Produto reservar(String id, int quantidade) {
        Produto produto = buscarSemCache(id);
        try {
            produto.reservar(quantidade);
            return repository.save(produto);
        } catch (OptimisticLockingFailureException e) {
            throw new ConflitoDeConcorrenciaException(
                    "conflito de concorrencia ao reservar produto " + id + ", tente novamente");
        }
    }

    @CacheEvict(
            value = {"produtos", "produto"},
            allEntries = true)
    public Produto liberar(String id, int quantidade) {
        Produto produto = buscarSemCache(id);
        produto.liberar(quantidade);
        return repository.save(produto);
    }

    @CacheEvict(
            value = {"produtos", "produto"},
            allEntries = true)
    public Produto confirmar(String id, int quantidade) {
        Produto produto = buscarSemCache(id);
        produto.confirmar(quantidade);
        return repository.save(produto);
    }

    private Produto buscarSemCache(String id) {
        return repository.findById(id).orElseThrow(() -> new ProdutoNaoEncontradoException(id));
    }

    public static class ProdutoNaoEncontradoException extends RuntimeException {
        public ProdutoNaoEncontradoException(String id) {
            super("produto nao encontrado: " + id);
        }
    }

    public static class ConflitoDeConcorrenciaException extends RuntimeException {
        public ConflitoDeConcorrenciaException(String message) {
            super(message);
        }
    }
}

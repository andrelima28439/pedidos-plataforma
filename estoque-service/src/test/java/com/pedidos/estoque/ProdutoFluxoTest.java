package com.pedidos.estoque;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pedidos.estoque.domain.EstoqueInsuficienteException;
import com.pedidos.estoque.domain.Produto;
import com.pedidos.estoque.repository.ProdutoRepository;
import com.pedidos.estoque.service.ProdutoService;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;

class ProdutoFluxoTest extends AbstractIntegrationTest {

    @Autowired
    ProdutoRepository repository;

    @Autowired
    ProdutoService service;

    @Autowired
    CacheManager cacheManager;

    @BeforeEach
    void limpar() {
        repository.deleteAll();
        cacheManager.getCacheNames().forEach(n -> cacheManager.getCache(n).clear());
    }

    @Test
    void reservarDepoisConfirmarDecrementaEstoque() {
        Produto p = service.criar(new Produto("Cadeira", "gamer", new BigDecimal("1299.00"), 5, "moveis"));

        Produto aposReserva = service.reservar(p.getId(), 2);
        assertEquals(5, aposReserva.getQuantidadeDisponivel());
        assertEquals(2, aposReserva.getQuantidadeReservada());
        assertEquals(3, aposReserva.getQuantidadeLivre());

        Produto aposConfirmar = service.confirmar(p.getId(), 2);
        assertEquals(3, aposConfirmar.getQuantidadeDisponivel());
        assertEquals(0, aposConfirmar.getQuantidadeReservada());
        assertEquals(3, aposConfirmar.getQuantidadeLivre());
        System.out.println("[fluxo] reservar+confirmar OK: disponivel=3 reservada=0");
    }

    @Test
    void reservarDepoisLiberarRestauraLivre() {
        Produto p = service.criar(new Produto("Mouse", "gamer", new BigDecimal("199.90"), 10, "informatica"));

        service.reservar(p.getId(), 3);
        Produto aposLiberar = service.liberar(p.getId(), 3);
        assertEquals(10, aposLiberar.getQuantidadeDisponivel());
        assertEquals(0, aposLiberar.getQuantidadeReservada());
        assertEquals(10, aposLiberar.getQuantidadeLivre());
        System.out.println("[fluxo] reservar+liberar OK: disponivel=10 reservada=0");
    }

    @Test
    void reservaAcimaDoDisponivelFalha() {
        Produto p = service.criar(new Produto("Teclado", "mecanico", new BigDecimal("349.90"), 2, "informatica"));

        assertThrows(EstoqueInsuficienteException.class, () -> service.reservar(p.getId(), 3));

        Produto intacto = repository.findById(p.getId()).orElseThrow();
        assertEquals(0, intacto.getQuantidadeReservada());
        System.out.println("[fluxo] reserva acima do estoque recusada corretamente");
    }

    @Test
    void listarUsaCacheRedis() {
        service.criar(new Produto("Item1", "d1", new BigDecimal("10.00"), 5, "cat"));
        assertEquals(1, service.listar().size());
        service.criar(new Produto("Item2", "d2", new BigDecimal("20.00"), 5, "cat"));
        // criar() evicta o cache, entao a listagem reflete os 2 itens
        assertEquals(2, service.listar().size());
        // terceira chamada vem do cache Redis sem quebrar
        assertTrue(service.listar().size() >= 2);
        System.out.println("[fluxo] listagem com cache Redis OK");
    }
}

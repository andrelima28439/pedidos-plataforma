package com.pedidos.estoque;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pedidos.estoque.domain.Produto;
import com.pedidos.estoque.repository.ProdutoRepository;
import com.pedidos.estoque.service.ProdutoService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;

/**
 * Secao 3.3 do roteiro: duas reservas simultaneas NAO podem vender
 * mais do que o estoque disponivel.
 *
 * Cenario: produto com 5 unidades, 2 threads tentam reservar 4 cada.
 * Esperado: exatamente 1 sucesso e 1 falha (409/otimista), estado final
 * reservada=4, livre=1, disponivel ainda 5 (reserva nao decrementa).
 */
class ReservaConcorrenteTest extends AbstractIntegrationTest {

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
    void duasReservasSimultaneasNaoUltrapassamEstoque() throws Exception {
        Produto produto = new Produto("Produto Corrida", "teste concorrencia", new BigDecimal("100.00"), 5, "teste");
        produto = repository.save(produto);
        String produtoId = produto.getId();

        int threads = 2;
        int quantidadePorReserva = 4;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch prontos = new CountDownLatch(threads);
        CountDownLatch largada = new CountDownLatch(1);
        AtomicInteger sucessos = new AtomicInteger();
        AtomicInteger falhas = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                prontos.countDown();
                try {
                    largada.await(10, TimeUnit.SECONDS);
                    service.reservar(produtoId, quantidadePorReserva);
                    sucessos.incrementAndGet();
                } catch (Exception e) {
                    System.out.println("[corrida] falha esperada em uma thread: "
                            + e.getClass().getSimpleName() + " - " + e.getMessage());
                    falhas.incrementAndGet();
                }
                return null;
            }));
        }

        assertTrue(prontos.await(30, TimeUnit.SECONDS), "threads nao ficaram prontas");
        largada.countDown();
        for (Future<?> f : futures) {
            f.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();

        System.out.println("[corrida] sucessos=" + sucessos.get() + " falhas=" + falhas.get());

        assertEquals(1, sucessos.get(), "exatamente 1 reserva deve ter sucesso");
        assertEquals(1, falhas.get(), "exatamente 1 reserva deve falhar");

        Produto final_ = repository.findById(produtoId).orElseThrow();
        System.out.println("[corrida] estado final: disponivel=" + final_.getQuantidadeDisponivel()
                + " reservada=" + final_.getQuantidadeReservada()
                + " livre=" + final_.getQuantidadeLivre()
                + " version=" + final_.getVersion());

        assertEquals(5, final_.getQuantidadeDisponivel(), "reserva nao decrementa estoque fisico");
        assertEquals(4, final_.getQuantidadeReservada(), "apenas 4 unidades podem estar reservadas");
        assertEquals(1, final_.getQuantidadeLivre(), "resta 1 unidade livre");
        assertTrue(
                final_.getQuantidadeReservada() <= final_.getQuantidadeDisponivel(),
                "nunca pode reservar mais do que o disponivel");
    }
}

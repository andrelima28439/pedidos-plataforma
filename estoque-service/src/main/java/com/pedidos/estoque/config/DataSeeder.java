package com.pedidos.estoque.config;

import com.pedidos.estoque.domain.Produto;
import com.pedidos.estoque.repository.ProdutoRepository;
import java.math.BigDecimal;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Seed inicial para demonstracao local (fora dos testes).
 */
@Configuration
public class DataSeeder {

    @Bean
    @Profile("!test")
    CommandLineRunner seed(ProdutoRepository repository) {
        return args -> {
            if (repository.count() == 0) {
                repository.save(new Produto(
                        "Teclado Mecanico", "Teclado RGB switch blue", new BigDecimal("349.90"), 20, "informatica"));
                repository.save(
                        new Produto("Mouse Gamer", "Mouse 16000 DPI", new BigDecimal("199.90"), 15, "informatica"));
                repository.save(
                        new Produto("Cadeira Gamer", "Cadeira ergonomica", new BigDecimal("1299.00"), 5, "moveis"));
            }
        };
    }
}

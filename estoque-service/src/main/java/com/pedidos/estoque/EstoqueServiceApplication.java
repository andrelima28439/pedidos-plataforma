package com.pedidos.estoque;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class EstoqueServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(EstoqueServiceApplication.class, args);
    }
}

package com.pedidos.estoque.repository;

import com.pedidos.estoque.domain.Produto;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProdutoRepository extends MongoRepository<Produto, String> {}

package com.pedidos.pedido.service;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class EstoqueClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public EstoqueClient(RestTemplate restTemplate, @Value("${estoque.url:http://localhost:8081}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public void reservar(String produtoId, int quantidade) {
        post("/produtos/" + produtoId + "/reservar", quantidade);
    }

    public void liberar(String produtoId, int quantidade) {
        post("/produtos/" + produtoId + "/liberar", quantidade);
    }

    public void confirmar(String produtoId, int quantidade) {
        post("/produtos/" + produtoId + "/confirmar", quantidade);
    }

    private void post(String path, int quantidade) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Integer>> body = new HttpEntity<>(Map.of("quantidade", quantidade), headers);
        restTemplate.postForEntity(baseUrl + path, body, String.class);
    }
}

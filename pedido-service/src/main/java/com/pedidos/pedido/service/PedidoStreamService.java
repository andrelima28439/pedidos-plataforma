package com.pedidos.pedido.service;

import com.pedidos.pedido.domain.PedidoStatus;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class PedidoStreamService {

    private static final Logger log = LoggerFactory.getLogger(PedidoStreamService.class);

    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter inscrever(String clienteId) {
        SseEmitter emitter = new SseEmitter(60_000L);
        emitters.computeIfAbsent(clienteId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remover(clienteId, emitter));
        emitter.onTimeout(() -> remover(clienteId, emitter));
        try {
            emitter.send(SseEmitter.event().name("conectado").data("ok"));
        } catch (Exception e) {
            remover(clienteId, emitter);
        }
        return emitter;
    }

    public void publicar(String clienteId, UUID pedidoId, PedidoStatus status) {
        List<SseEmitter> lista = emitters.getOrDefault(clienteId, List.of());
        for (SseEmitter e : lista) {
            try {
                e.send(SseEmitter.event()
                        .name("pedido")
                        .data(Map.of("pedidoId", pedidoId.toString(), "status", status.name())));
            } catch (Exception ex) {
                log.debug("falha ao enviar SSE, removendo emitter");
                remover(clienteId, e);
            }
        }
    }

    private void remover(String clienteId, SseEmitter emitter) {
        List<SseEmitter> lista = emitters.get(clienteId);
        if (lista != null) {
            lista.remove(emitter);
        }
    }
}

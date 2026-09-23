package com.pedidos.pedido.service;

import com.pedidos.pedido.domain.Pedido;
import com.pedidos.pedido.domain.PedidoItem;
import com.pedidos.pedido.domain.PedidoStatus;
import com.pedidos.pedido.events.PedidoEvents.ItemEvento;
import com.pedidos.pedido.events.PedidoEvents.PedidoCriadoEvent;
import com.pedidos.pedido.repository.PedidoRepository;
import com.pedidos.pedido.web.PedidoDtos.CriarPedidoRequest;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PedidoService {

    private static final Logger log = LoggerFactory.getLogger(PedidoService.class);

    private final PedidoRepository repository;
    private final EstoqueClient estoque;
    private final PedidoEventPublisher publisher;
    private final PedidoStreamService stream;

    public PedidoService(
            PedidoRepository repository,
            EstoqueClient estoque,
            PedidoEventPublisher publisher,
            PedidoStreamService stream) {
        this.repository = repository;
        this.estoque = estoque;
        this.publisher = publisher;
        this.stream = stream;
    }

    @Transactional
    public Pedido criar(String clienteId, CriarPedidoRequest request) {
        List<PedidoItem> itens = request.itens().stream()
                .map(i -> new PedidoItem(i.produtoId(), i.quantidade(), i.precoUnitario()))
                .toList();

        // 1. reserva sincronica no estoque-service (orquestrado por REST — ver ADR)
        for (PedidoItem item : itens) {
            estoque.reservar(item.getProdutoId(), item.getQuantidade());
        }

        // 2. salva CRIADO e avanca para AGUARDANDO_PAGAMENTO (transicao explicita)
        Pedido pedido = new Pedido(clienteId, itens);
        pedido.avancarPara(PedidoStatus.AGUARDANDO_PAGAMENTO, "estoque reservado");
        repository.save(pedido);

        // 3. publica pedido.criado (pagamento-service consome).
        // correlation-id = pedidoId: rastreia o pedido de ponta a ponta nos logs JSON.
        String eventId = UUID.randomUUID().toString();
        PedidoCriadoEvent evento = new PedidoCriadoEvent(
                eventId,
                pedido.getId(),
                clienteId,
                pedido.getValorTotal(),
                itens.stream()
                        .map(i -> new ItemEvento(i.getProdutoId(), i.getQuantidade(), i.getPrecoUnitario()))
                        .toList(),
                pedido.getId().toString());
        MDC.put("correlationId", evento.correlationId());
        try {
            log.info(
                    "pedido criado pedidoId={} eventId={} correlationId={}",
                    pedido.getId(),
                    eventId,
                    evento.correlationId());
            publisher.publicarPedidoCriado(evento);
        } catch (Exception e) {
            log.error("falha ao publicar pedido.criado pedidoId={}", pedido.getId(), e);
            throw e;
        } finally {
            MDC.remove("correlationId");
        }
        stream.publicar(clienteId, pedido.getId(), pedido.getStatus());
        return pedido;
    }

    @Transactional(readOnly = true)
    public List<Pedido> listarDoCliente(String clienteId) {
        return repository.findByClienteIdOrderByCriadoEmDesc(clienteId);
    }

    @Transactional(readOnly = true)
    public Pedido buscarDoCliente(String clienteId, UUID pedidoId) {
        Pedido pedido = repository.findById(pedidoId).orElseThrow(() -> new PedidoNaoEncontradoException(pedidoId));
        if (!pedido.getClienteId().equals(clienteId)) {
            throw new PedidoNaoEncontradoException(pedidoId);
        }
        return pedido;
    }

    public static class PedidoNaoEncontradoException extends RuntimeException {
        public PedidoNaoEncontradoException(UUID id) {
            super("pedido nao encontrado: " + id);
        }
    }
}

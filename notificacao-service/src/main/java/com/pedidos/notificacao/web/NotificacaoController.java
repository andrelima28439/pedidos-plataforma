package com.pedidos.notificacao.web;

import static com.pedidos.notificacao.config.RabbitConfig.DLQ_PAGAMENTO_APROVADO;
import static com.pedidos.notificacao.config.RabbitConfig.DLQ_PAGAMENTO_RECUSADO;
import static com.pedidos.notificacao.config.RabbitConfig.DLQ_PEDIDO_CRIADO;

import com.pedidos.notificacao.domain.NotificacaoProcessada;
import com.pedidos.notificacao.repository.NotificacaoProcessadaRepository;
import java.util.List;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notificacoes")
public class NotificacaoController {

    private final NotificacaoProcessadaRepository repository;
    private final RabbitTemplate rabbit;

    public NotificacaoController(NotificacaoProcessadaRepository repository, RabbitTemplate rabbit) {
        this.repository = repository;
        this.rabbit = rabbit;
    }

    @GetMapping("/processadas")
    public List<NotificacaoProcessada> processadas() {
        return repository.findAll();
    }

    @GetMapping("/dlq/tamanhos")
    public DlqTamanhos dlqTamanhos() {
        return new DlqTamanhos(
                tamanho(DLQ_PEDIDO_CRIADO), tamanho(DLQ_PAGAMENTO_APROVADO), tamanho(DLQ_PAGAMENTO_RECUSADO));
    }

    private int tamanho(String fila) {
        var props = rabbit.execute(channel -> channel.queueDeclarePassive(fila));
        return props != null ? props.getMessageCount() : -1;
    }

    public record DlqTamanhos(int pedidoCriado, int pagamentoAprovado, int pagamentoRecusado) {}
}

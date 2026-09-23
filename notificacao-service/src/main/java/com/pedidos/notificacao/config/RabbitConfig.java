package com.pedidos.notificacao.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topologia RabbitMQ:
 * - exchange notificacao.exchange (direct) com 3 filas, uma por tipo
 * - cada fila tem x-dead-letter-exchange -> notificacao.dlx
 * - DLX (direct) com 3 DLQs, uma por fila de origem
 * Falha de envio com reject sem requeue cai na DLQ correspondente.
 */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "notificacao.exchange";
    public static final String DLX = "notificacao.dlx";

    public static final String FILA_PEDIDO_CRIADO = "notificacao.pedido-criado";
    public static final String FILA_PAGAMENTO_APROVADO = "notificacao.pagamento-aprovado";
    public static final String FILA_PAGAMENTO_RECUSADO = "notificacao.pagamento-recusado";

    public static final String DLQ_PEDIDO_CRIADO = "notificacao.pedido-criado.dlq";
    public static final String DLQ_PAGAMENTO_APROVADO = "notificacao.pagamento-aprovado.dlq";
    public static final String DLQ_PAGAMENTO_RECUSADO = "notificacao.pagamento-recusado.dlq";

    @Bean
    DirectExchange exchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    MessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    DirectExchange dlx() {
        return new DirectExchange(DLX, true, false);
    }

    private Queue filaComDlq(String nome, String routingDlq) {
        return QueueBuilder.durable(nome)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", routingDlq)
                .build();
    }

    @Bean
    Queue filaPedidoCriado() {
        return filaComDlq(FILA_PEDIDO_CRIADO, FILA_PEDIDO_CRIADO);
    }

    @Bean
    Queue filaPagamentoAprovado() {
        return filaComDlq(FILA_PAGAMENTO_APROVADO, FILA_PAGAMENTO_APROVADO);
    }

    @Bean
    Queue filaPagamentoRecusado() {
        return filaComDlq(FILA_PAGAMENTO_RECUSADO, FILA_PAGAMENTO_RECUSADO);
    }

    @Bean
    Queue dlqPedidoCriado() {
        return QueueBuilder.durable(DLQ_PEDIDO_CRIADO).build();
    }

    @Bean
    Queue dlqPagamentoAprovado() {
        return QueueBuilder.durable(DLQ_PAGAMENTO_APROVADO).build();
    }

    @Bean
    Queue dlqPagamentoRecusado() {
        return QueueBuilder.durable(DLQ_PAGAMENTO_RECUSADO).build();
    }

    @Bean
    Binding bindPedidoCriado(Queue filaPedidoCriado, DirectExchange exchange) {
        return BindingBuilder.bind(filaPedidoCriado).to(exchange).with(FILA_PEDIDO_CRIADO);
    }

    @Bean
    Binding bindPagamentoAprovado(Queue filaPagamentoAprovado, DirectExchange exchange) {
        return BindingBuilder.bind(filaPagamentoAprovado).to(exchange).with(FILA_PAGAMENTO_APROVADO);
    }

    @Bean
    Binding bindPagamentoRecusado(Queue filaPagamentoRecusado, DirectExchange exchange) {
        return BindingBuilder.bind(filaPagamentoRecusado).to(exchange).with(FILA_PAGAMENTO_RECUSADO);
    }

    @Bean
    Binding bindDlqPedidoCriado(Queue dlqPedidoCriado, DirectExchange dlx) {
        return BindingBuilder.bind(dlqPedidoCriado).to(dlx).with(FILA_PEDIDO_CRIADO);
    }

    @Bean
    Binding bindDlqPagamentoAprovado(Queue dlqPagamentoAprovado, DirectExchange dlx) {
        return BindingBuilder.bind(dlqPagamentoAprovado).to(dlx).with(FILA_PAGAMENTO_APROVADO);
    }

    @Bean
    Binding bindDlqPagamentoRecusado(Queue dlqPagamentoRecusado, DirectExchange dlx) {
        return BindingBuilder.bind(dlqPagamentoRecusado).to(dlx).with(FILA_PAGAMENTO_RECUSADO);
    }
}

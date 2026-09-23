package com.pedidos.pagamento.aws;

import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

/**
 * Regra de negocio: pedidos com valor >= LIMITE_ALTO_VALOR (padrao 1000.00)
 * vao para a fila SQS pedidos-alto-valor para revisao manual (antifraude).
 * Justifica o uso do SQS como fila alternativa ao Kafka para um caso
 * especifico de revisao humana.
 */
@Service
public class AltoValorService {

    private static final Logger log = LoggerFactory.getLogger(AltoValorService.class);

    public static final String FILA = "pedidos-alto-valor";

    private final SqsClient sqs;
    private final BigDecimal limite;

    public AltoValorService(SqsClient sqs, @Value("${aws.alto-valor-limite:1000.00}") BigDecimal limite) {
        this.sqs = sqs;
        this.limite = limite;
    }

    public boolean isAltoValor(BigDecimal valor) {
        return valor.compareTo(limite) >= 0;
    }

    public BigDecimal getLimite() {
        return limite;
    }

    public String garantirFila() {
        try {
            return sqs.getQueueUrl(GetQueueUrlRequest.builder().queueName(FILA).build())
                    .queueUrl();
        } catch (QueueDoesNotExistException e) {
            String url = sqs.createQueue(
                            CreateQueueRequest.builder().queueName(FILA).build())
                    .queueUrl();
            log.info("[sqs] fila criada: {} -> {}", FILA, url);
            return url;
        }
    }

    public void enviarParaRevisao(String eventId, String pedidoId, BigDecimal valor, String correlationId) {
        if (!isAltoValor(valor)) {
            return;
        }
        String url = garantirFila();
        String corpo =
                """
                {"eventId":"%s","pedidoId":"%s","valor":"%s","motivo":"alto-valor-para-revisao-manual","correlationId":"%s"}"""
                        .formatted(eventId, pedidoId, valor.toPlainString(), correlationId);
        sqs.sendMessage(
                SendMessageRequest.builder().queueUrl(url).messageBody(corpo).build());
        log.info("[sqs] pedido {} valor {} enviado para {} (revisao manual)", pedidoId, valor, FILA);
    }

    public List<Message> receberParaTeste() {
        String url = garantirFila();
        return sqs.receiveMessage(ReceiveMessageRequest.builder()
                        .queueUrl(url)
                        .maxNumberOfMessages(10)
                        .waitTimeSeconds(2)
                        .build())
                .messages();
    }
}

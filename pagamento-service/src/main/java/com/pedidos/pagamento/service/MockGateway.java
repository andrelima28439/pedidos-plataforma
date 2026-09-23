package com.pedidos.pagamento.service;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Mock de gateway de pagamento.
 * Regra de negocio documentada: valores terminados em .99 sao RECUSADOS
 * (para demonstrar os dois fluxos); demais sao APROVADOS.
 * Falhas transitorias sao injetaveis para os testes de retry/DLQ.
 */
@Component
public class MockGateway {

    private static final Logger log = LoggerFactory.getLogger(MockGateway.class);

    private final AtomicInteger falharProximas = new AtomicInteger(0);
    private final AtomicBoolean sempreFalhar = new AtomicBoolean(false);

    public enum Resultado {
        APROVADO,
        RECUSADO
    }

    public Resultado cobrar(BigDecimal valor) {
        if (sempreFalhar.get() || falharProximas.getAndDecrement() > 0) {
            log.warn("mock gateway indisponivel (transitoria) valor={}", valor);
            throw new TransientGatewayException("gateway indisponivel (simulado)");
        }
        if (valor.toPlainString().endsWith(".99")) {
            log.info("mock gateway RECUSADO valor={} (regra .99)", valor);
            return Resultado.RECUSADO;
        }
        log.info("mock gateway APROVADO valor={}", valor);
        return Resultado.APROVADO;
    }

    /** Testes: faz as proximas N chamadas falharem com erro transitorio. */
    public void falharProximas(int n) {
        falharProximas.set(n);
    }

    /** Testes: liga/desliga falha definitiva. */
    public void setSempreFalhar(boolean v) {
        sempreFalhar.set(v);
    }

    public static class TransientGatewayException extends RuntimeException {
        public TransientGatewayException(String message) {
            super(message);
        }
    }
}

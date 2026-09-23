package com.pedidos.pagamento.web;

import com.pedidos.pagamento.service.MockGateway;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Helper DEV-ONLY para demonstrações (Postman) e testes manuais: força o mock
 * gateway a falhar e encher a DLQ de verdade. Nunca expor em produção
 * (restringir por profile/segurança — ver Limitações conhecidas).
 */
@RestController
@RequestMapping("/pagamentos/admin")
public class PagamentoAdminController {

    private final MockGateway gateway;

    public PagamentoAdminController(MockGateway gateway) {
        this.gateway = gateway;
    }

    @PostMapping("/gateway/falhar-proximas")
    public Map<String, Object> falharProximas(@RequestParam(defaultValue = "3") int n) {
        gateway.falharProximas(n);
        return Map.of("falharProximas", n);
    }

    @PostMapping("/gateway/sempre-falhar")
    public Map<String, Object> sempreFalhar(@RequestParam(defaultValue = "true") boolean ativo) {
        gateway.setSempreFalhar(ativo);
        return Map.of("sempreFalhar", ativo);
    }
}

package com.pedidos.pedido.web;

import com.pedidos.pedido.service.PedidoService;
import com.pedidos.pedido.service.PedidoStreamService;
import com.pedidos.pedido.web.PedidoDtos.CriarPedidoRequest;
import com.pedidos.pedido.web.PedidoDtos.PedidoResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/pedidos")
public class PedidoController {

    private final PedidoService service;
    private final PedidoStreamService stream;

    public PedidoController(PedidoService service, PedidoStreamService stream) {
        this.service = service;
        this.stream = stream;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PedidoResponse criar(Authentication auth, @Valid @RequestBody CriarPedidoRequest request) {
        return PedidoResponse.from(service.criar(auth.getName(), request));
    }

    @GetMapping
    public List<PedidoResponse> listar(Authentication auth) {
        return service.listarDoCliente(auth.getName()).stream()
                .map(PedidoResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public PedidoResponse detalhar(Authentication auth, @PathVariable UUID id) {
        return PedidoResponse.from(service.buscarDoCliente(auth.getName(), id));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(Authentication auth) {
        return stream.inscrever(auth.getName());
    }
}

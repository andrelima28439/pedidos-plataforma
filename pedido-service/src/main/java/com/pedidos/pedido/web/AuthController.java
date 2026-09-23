package com.pedidos.pedido.web;

import com.pedidos.pedido.security.JwtService;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final JwtService jwt;

    public AuthController(JwtService jwt) {
        this.jwt = jwt;
    }

    public record TokenRequest(@NotBlank String clienteId) {}

    @PostMapping("/token")
    public Map<String, String> token(@RequestBody TokenRequest request) {
        return Map.of("token", jwt.gerar(request.clienteId()));
    }
}

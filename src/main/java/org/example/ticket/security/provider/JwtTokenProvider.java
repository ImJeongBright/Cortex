package org.example.ticket.security.provider;

import lombok.RequiredArgsConstructor;
import org.example.ticket.security.jwt.JwtUtil;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtUtil jwtUtil;

    public Map<String, String> provideJwt(String walletAddress, String role) {
        String token = jwtUtil.createJwt(walletAddress, role, 100000 * 60 * 60L);
        Map<String, String> jwt = new HashMap<>();

        jwt.put("token", token);
        jwt.put("walletAddress", walletAddress);
        jwt.put("role", role);

        return jwt;
    }
}


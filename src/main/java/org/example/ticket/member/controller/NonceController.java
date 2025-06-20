package org.example.ticket.member.controller;

import lombok.RequiredArgsConstructor;
import org.example.ticket.member.service.MemberService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class NonceController {

    private final MemberService service;

    @GetMapping("/api/user/nonce")
    public Map<String, Integer> getNonce(@RequestParam String walletAddress) {
        Integer userNonce = service.getOrCreateNonce(walletAddress);
        return Collections.singletonMap("nonce", userNonce);
    }
}
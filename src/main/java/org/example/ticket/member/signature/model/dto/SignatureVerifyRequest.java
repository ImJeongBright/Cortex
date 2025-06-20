package org.example.ticket.member.signature.model.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SignatureVerifyRequest {
    private String walletAddress;
    private String message;
    private String signature;
}

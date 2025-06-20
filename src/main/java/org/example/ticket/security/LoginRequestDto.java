package org.example.ticket.security;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequestDto {
    private String walletAddress;
    private String signature;
}

package org.example.ticket.security.token;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

public class MetamaskAuthenticationToken extends UsernamePasswordAuthenticationToken {

    public MetamaskAuthenticationToken(String walletAddress, String signature) {
        super(walletAddress, signature);
    }

    public String getAddress() {
        return (String) super.getPrincipal();
    }

    public String getSignature() {
        return (String) super.getCredentials();
    }

}

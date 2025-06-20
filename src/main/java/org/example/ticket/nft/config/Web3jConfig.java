package org.example.ticket.nft.config;

import org.example.ticket.nft.contract.Nft;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.StaticGasProvider;

import java.io.IOException;
import java.math.BigInteger;

@Configuration
public class Web3jConfig {

    @Value("${infura.url}")
    private String nodeUrl;

    @Value("${ethereum.wallet.private-key}")
    private String privateKey;

    @Value("${solidity.contract.address}")
    private String contractAddress;

    @Bean
    public Nft contract() throws IOException {
        return Nft.load(
                contractAddress,
                web3j(),
                Credentials.create(privateKey),
                new StaticGasProvider(
                        web3j().ethGasPrice().send().getGasPrice(),
                        BigInteger.valueOf(2_100_000L)
                )
        );
    }

    @Bean
    public Web3j web3j() {
        return Web3j.build(new HttpService(nodeUrl));
    }
}

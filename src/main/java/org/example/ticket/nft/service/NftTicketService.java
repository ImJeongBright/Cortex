package org.example.ticket.nft.service;

import lombok.RequiredArgsConstructor;
import org.example.ticket.nft.contract.Nft;
import org.example.ticket.nft.request.NftTicketBuyRequest;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;

import java.math.BigInteger;
import java.util.logging.Logger;

@Service
@RequiredArgsConstructor
public class NftTicketService {

    private final Logger logger = Logger.getLogger(getClass().getName());

    private final Web3j web3j;
    private final Nft contract;

    public void buyTicket(NftTicketBuyRequest request) {
        try {
            if(!contract.isValid()) {
                throw new IllegalStateException("Contract is not valid");
            }
            createNFT(request.getTo(), uploadToIpfs(request));
        } catch (Exception e) {
            logger.severe("Error interacting with the contract: " + e.getMessage());
            throw new IllegalStateException(e);
        } finally {
            web3j.shutdown();
        }
    }

    // NFT 발행 트랜잭션은 시간이 오래 걸리므로 비동기로 처리
    // 따라서 사용자는 발행이 완료되기까지 계속 기다릴 필요가 없음
    private void createNFT(String to, String tokenURI) {
        contract.mintNFT(to, tokenURI).sendAsync().thenApply(receipt -> {
            if (!receipt.isStatusOK()) {
                throw new RuntimeException("Transaction failed");
            }
            System.out.println("Transaction successful: " + receipt.getTransactionHash());
            contract.getTransferEvents(receipt).forEach(event -> {
                BigInteger tokenId = event.tokenId;
                System.out.println("tokenId = " + tokenId);
                // TODO: DB에 NFT 정보 저장
            });
            return receipt;
        });
    }

    private String uploadToIpfs(NftTicketBuyRequest request) {
        // TODO: request 내부에 있는 티켓 정보를 가공해서 IPFS 업로드 및 URL 반환
        return "ipfs://tempUrl"; // 임시 URL
    }
}

package org.example.ticket.nft.controller;

import lombok.RequiredArgsConstructor;
import org.example.ticket.nft.request.NftTicketBuyRequest;
import org.example.ticket.nft.service.NftTicketService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/nft/ticket")
public class NftTicketController {

    private final NftTicketService nftTicketService;

    @PostMapping("/buy")
    public ResponseEntity<Void> buyTicket(@RequestBody NftTicketBuyRequest request) {
        nftTicketService.buyTicket(request);
        return ResponseEntity.ok().build();
    }

}

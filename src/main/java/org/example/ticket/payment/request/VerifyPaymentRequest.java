package org.example.ticket.payment.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyPaymentRequest {

    private String orderUID;
    private String paymentUID;

}

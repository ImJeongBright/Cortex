package org.example.ticket.payment.response;

import jakarta.persistence.Entity;
import lombok.*;
import org.example.ticket.util.constant.PaymentStatus;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class VerifyPaymentResponse {


    private Integer price;
    private PaymentStatus status;
    private String paymentUid;


    public static VerifyPaymentResponse from (Integer price, String paymentUid) {
        return VerifyPaymentResponse.builder()
                .price(price)
                .status(PaymentStatus.SUCCESS)
                .paymentUid(paymentUid)
                .build();
    }

}

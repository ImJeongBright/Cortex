package org.example.ticket.payment.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.example.ticket.payment.response.VerifyPaymentResponse;
import org.example.ticket.reservation.model.Reservation;
import org.example.ticket.util.constant.PaymentStatus;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Integer price;
    private PaymentStatus status;
    private String paymentUid;

    @OneToOne(mappedBy = "payment")
    private Reservation reservation;

    public static Settlement from(VerifyPaymentResponse response) {
        return Settlement.builder()
                .price(response.getPrice())
                .status(response.getStatus())
                .paymentUid(response.getPaymentUid())
                .build();
    }

    public void changePaymentStatus(PaymentStatus paymentStatus) {
        this.status = paymentStatus;
    }

}

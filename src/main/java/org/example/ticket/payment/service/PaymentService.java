package org.example.ticket.payment.service;

import com.siot.IamportRestClient.IamportClient;
import com.siot.IamportRestClient.exception.IamportResponseException;
import com.siot.IamportRestClient.request.CancelData;
import com.siot.IamportRestClient.response.IamportResponse;
import com.siot.IamportRestClient.response.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ticket.payment.model.Settlement;
import org.example.ticket.payment.repository.SettlementRepository;
import org.example.ticket.payment.request.VerifyPaymentRequest;
import org.example.ticket.payment.response.VerifyPaymentResponse;
import org.example.ticket.util.constant.PaymentStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    private final IamportClient iamportClient;
    private final SettlementRepository settlementRepository;


    public IamportResponse<Payment> verifyPayment(VerifyPaymentRequest paymentRequest, Integer price) throws IamportResponseException, IOException {


        IamportResponse<Payment> paymentIamportResponse = iamportClient.paymentByImpUid(paymentRequest.getPaymentUID());

        Integer paymentPrice = paymentIamportResponse.getResponse().getAmount().intValue();

        if(!paymentPrice.equals(price)) {
            iamportClient.cancelPaymentByImpUid(new CancelData(paymentIamportResponse.getResponse().getImpUid(), true, new BigDecimal(paymentPrice)));

            throw new RuntimeException("위변조가 의심됩니다."); // custom Exception
        }


        return paymentIamportResponse;
    }


    public Settlement initialPayment(VerifyPaymentRequest paymentRequest, Integer price) throws IamportResponseException, IOException {

        IamportResponse<Payment> paymentIamportResponse = verifyPayment(paymentRequest, price);

        if(paymentIamportResponse == null) throw new RuntimeException("ㅇㅇ"); // custom exception;

        VerifyPaymentResponse verifyPaymentResponse = VerifyPaymentResponse.from(price, paymentRequest.getPaymentUID());

        Settlement settlement = Settlement.from(verifyPaymentResponse);
        return settlementRepository.save(settlement);
    }

}

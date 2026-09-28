package com.hopper.hts.fintech.flights.sdk.payments;

import com.hopper.hts.fintech.flights.cfar.model.Identifier;
import com.hopper.hts.fintech.flights.cfar.model.NotProvided;
import com.hopper.hts.fintech.flights.cfar.model.PaymentCard;
import com.hopper.hts.fintech.flights.cfar.model.PaymentInformation;
import com.hopper.hts.fintech.flights.cfar.model.Provided;
import com.hopper.hts.fintech.flights.cfar.model.ProvidedPayment;
import com.hopper.hts.fintech.flights.cfar.model.Token;

import java.util.Arrays;
import java.util.List;

public final class Payments {
    private Payments() {
    }

    public static ProvidedPayment tokenizedCard(String amount, String token) {
        Token identifier = new Token().type(Token.TypeEnum.TOKEN).value(token);
        PaymentCard card = new PaymentCard()
            .type(PaymentCard.TypeEnum.PAYMENT_CARD)
            .amount(amount)
            .identifier(new Identifier(identifier));
        return new ProvidedPayment(card);
    }

    public static PaymentInformation provided(ProvidedPayment... paymentMethods) {
        return provided(Arrays.asList(paymentMethods));
    }

    public static PaymentInformation provided(List<ProvidedPayment> paymentMethods) {
        Provided provided = new Provided().type(Provided.TypeEnum.PROVIDED).paymentMethods(paymentMethods);
        return new PaymentInformation(provided);
    }

    public static PaymentInformation notProvided(String amount) {
        NotProvided notProvided = new NotProvided().type(NotProvided.TypeEnum.NOT_PROVIDED).amount(amount);
        return new PaymentInformation(notProvided);
    }
}

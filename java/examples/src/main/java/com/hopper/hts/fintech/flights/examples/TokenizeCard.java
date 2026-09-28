package com.hopper.hts.fintech.flights.examples;

import com.hopper.hts.fintech.flights.sdk.payments.CardDetails;
import com.hopper.hts.fintech.flights.sdk.payments.SpreedlyTokenizer;

public final class TokenizeCard {
    public static void main(String[] args) throws Exception {
        SpreedlyTokenizer tokenizer = Env.spreedlyTokenizerOrNull();
        if (tokenizer == null) {
            System.err.println("SPREEDLY_ENVIRONMENT_KEY and SPREEDLY_ACCESS_SECRET are required");
            System.exit(2);
        }
        String token = tokenizer.tokenize(CardDetails.builder()
            .firstName("Ada")
            .lastName("Lovelace")
            .number("4111111111111111")
            .expirationMonth("12")
            .expirationYear("2030")
            .verificationValue("123")
            .build());
        System.out.printf("Spreedly payment method token: %s%n", token);
    }
}

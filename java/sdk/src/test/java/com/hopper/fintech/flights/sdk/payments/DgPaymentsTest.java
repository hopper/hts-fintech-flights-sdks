package com.hopper.fintech.flights.sdk.payments;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hopper.fintech.flights.dafar.JSON;
import com.hopper.fintech.flights.dafar.model.PaymentInformation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DgPaymentsTest {
    @Test
    void serializesTokenizedCardAsProvidedPaymentInformation() {
        PaymentInformation info = DgPayments.provided(DgPayments.tokenizedCard("123.45", "pm_abc"));

        JsonObject json = JsonParser.parseString(JSON.serialize(info)).getAsJsonObject();

        assertEquals("provided", json.get("type").getAsString());
        JsonObject card = json.getAsJsonArray("payment_methods").get(0).getAsJsonObject();
        assertEquals("payment_card", card.get("type").getAsString());
        assertEquals("123.45", card.get("amount").getAsString());
        assertEquals("token", card.getAsJsonObject("identifier").get("type").getAsString());
        assertEquals("pm_abc", card.getAsJsonObject("identifier").get("value").getAsString());
    }
}

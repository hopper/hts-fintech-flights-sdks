package com.hopper.fintech.flights.examples;

import com.hopper.fintech.flights.dafar.model.AncillaryItinerary;
import com.hopper.fintech.flights.dafar.model.Api;
import com.hopper.fintech.flights.dafar.model.BillingInfo;
import com.hopper.fintech.flights.dafar.model.Cabin;
import com.hopper.fintech.flights.dafar.model.DafarContract;
import com.hopper.fintech.flights.dafar.model.DafarOffer;
import com.hopper.fintech.flights.dafar.model.Channel;
import com.hopper.fintech.flights.dafar.model.ConfirmContractRequest;
import com.hopper.fintech.flights.dafar.model.ContactInfo;
import com.hopper.fintech.flights.dafar.model.CreateDraftContractRequest;
import com.hopper.fintech.flights.dafar.model.CreateOffersRequest;
import com.hopper.fintech.flights.dafar.model.EmailAddress;
import com.hopper.fintech.flights.dafar.model.ItineraryPassenger;
import com.hopper.fintech.flights.dafar.model.ItinerarySlice;
import com.hopper.fintech.flights.dafar.model.ItinerarySliceSegment;
import com.hopper.fintech.flights.dafar.model.Literal;
import com.hopper.fintech.flights.dafar.model.MerchandisingContext;
import com.hopper.fintech.flights.dafar.model.MerchantOfRecord;
import com.hopper.fintech.flights.dafar.model.OffersEligible;
import com.hopper.fintech.flights.dafar.model.PassengerType;
import com.hopper.fintech.flights.dafar.model.PaymentInformation;
import com.hopper.fintech.flights.dafar.model.PreBooking;
import com.hopper.fintech.flights.dafar.model.PriceOffersRequest;
import com.hopper.fintech.flights.sdk.FlightsDg;
import com.hopper.fintech.flights.sdk.payments.CardDetails;
import com.hopper.fintech.flights.sdk.payments.DgPayments;
import com.hopper.fintech.flights.sdk.payments.SpreedlyTokenizer;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class CreateDgContract {
    public static void main(String[] args) throws Exception {
        FlightsDg dg = Env.htsClient().dg();
        String sessionId = UUID.randomUUID().toString();

        AncillaryItinerary itinerary = sampleItinerary();
        OffersEligible offers = dg.offers().postOffers(offersRequest(sessionId, itinerary)).execute();
        if (offers == null || offers.getOffers().isEmpty()) {
            System.out.println("No Disruption Guarantee offer available for this itinerary");
            return;
        }
        DafarOffer unpriced = offers.getOffers().get(0);
        System.out.printf("Offer %s %s, rebooking allowance %s per passenger, priced=%s%n",
            unpriced.getPrice().getTotalAmount(), unpriced.getCurrency(), unpriced.getRebookingAllowancePerPassenger(), unpriced.getPriced());

        OffersEligible priced = dg.offers().postOffersPrice(new PriceOffersRequest()
            .offerIds(Collections.singletonList(unpriced.getId()))
            .currency(unpriced.getCurrency())
            .itinerary(itinerary)
            .userId("user-42")).execute();
        DafarOffer offer = priced.getOffers().get(0);
        System.out.printf("Priced offer %s %s, priced=%s%n", offer.getPrice().getTotalAmount(), offer.getCurrency(), offer.getPriced());

        DafarContract draft = dg.contracts().postContracts(UUID.randomUUID(), contractRequest(offer)).execute();
        System.out.printf("Draft contract %s (%s)%n", draft.getId(), draft.getStatus());

        dg.contracts().putContractsIdConfirm(draft.getId(), new ConfirmContractRequest().bookingReferences(bookingReferences())).execute();
        DafarContract confirmed = dg.contracts().getContractsId(draft.getId()).execute();
        System.out.printf("Confirmed contract %s (%s)%n", confirmed.getId(), confirmed.getStatus());
    }

    static Map<String, String> bookingReferences() {
        Map<String, String> references = new LinkedHashMap<>();
        for (String pair : Env.optional("HTS_BOOKING_REFERENCES", "pnr=ABC123").split(",")) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                references.put(pair.substring(0, eq).trim(), pair.substring(eq + 1).trim());
            }
        }
        return references;
    }

    static AncillaryItinerary sampleItinerary() {
        ItinerarySliceSegment outbound = new ItinerarySliceSegment()
            .originAirport("YYZ")
            .destinationAirport("YVR")
            .departureDateTime("2026-12-10T08:00")
            .arrivalDateTime("2026-12-10T10:15")
            .cabin(Cabin.ECONOMY)
            .marketingCarrierCode("WS")
            .marketingFlightNumber("701");

        return new AncillaryItinerary()
            .slices(Collections.singletonList(new ItinerarySlice()
                .segments(Collections.singletonList(outbound))
                .fareBrand("econo")))
            .passengers(Collections.singletonList(new ItineraryPassenger()
                .type(PassengerType.ADULT)
                .fareAmount("249.00")
                .ancillaries(Collections.emptyList())))
            .ancillaries(Collections.emptyList());
    }

    static CreateOffersRequest offersRequest(String sessionId, AncillaryItinerary itinerary) {
        return new CreateOffersRequest()
            .sessionId(sessionId)
            .currency("CAD")
            .pointOfSale("CA")
            .itinerary(itinerary)
            .channel(new Channel(new Api().type(Api.TypeEnum.API).client("example-app")))
            .merchandisingContext(new MerchandisingContext(new PreBooking().type(PreBooking.TypeEnum.PRE_BOOKING)));
    }

    static CreateDraftContractRequest contractRequest(DafarOffer offer) throws Exception {
        String amount = offer.getPrice().getTotalAmount();
        SpreedlyTokenizer tokenizer = Env.spreedlyTokenizerOrNull();
        String presetToken = Env.optional("HTS_PAYMENT_TOKEN", "");

        MerchantOfRecord merchantOfRecord;
        PaymentInformation paymentInformation;
        if (!presetToken.isEmpty()) {
            merchantOfRecord = MerchantOfRecord.HTS;
            paymentInformation = DgPayments.provided(DgPayments.tokenizedCard(amount, presetToken));
        } else if (tokenizer == null) {
            merchantOfRecord = MerchantOfRecord.PARTNER;
            paymentInformation = DgPayments.notProvided(amount);
        } else {
            merchantOfRecord = MerchantOfRecord.HTS;
            String token = tokenizer.tokenize(CardDetails.builder()
                .firstName("Ada")
                .lastName("Lovelace")
                .number("4111111111111111")
                .expirationMonth("12")
                .expirationYear("2030")
                .verificationValue("123")
                .build());
            paymentInformation = DgPayments.provided(DgPayments.tokenizedCard(amount, token));
        }

        return new CreateDraftContractRequest()
            .offerId(offer.getId())
            .currency(offer.getCurrency())
            .locale("en-CA")
            .merchantOfRecord(merchantOfRecord)
            .paymentInformation(paymentInformation)
            .contactInfo(new ContactInfo()
                .emailAddress(new EmailAddress(new Literal().type(Literal.TypeEnum.LITERAL).value("ada@example.com"))))
            .billingInfo(new BillingInfo()
                .firstName("Ada")
                .lastName("Lovelace")
                .streetAddress("1 Front St W")
                .city("Toronto")
                .stateProvince("ON")
                .postalCode("M5J 2X5")
                .country("CA"))
            .userId("user-42");
    }
}

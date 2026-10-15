package com.hopper.hts.fintech.flights.examples;

import com.hopper.hts.fintech.flights.cfar.model.AncillaryItinerary;
import com.hopper.hts.fintech.flights.cfar.model.Api;
import com.hopper.hts.fintech.flights.cfar.model.BillingInfo;
import com.hopper.hts.fintech.flights.cfar.model.Cabin;
import com.hopper.hts.fintech.flights.cfar.model.CfarContract;
import com.hopper.hts.fintech.flights.cfar.model.CfarOffer;
import com.hopper.hts.fintech.flights.cfar.model.Channel;
import com.hopper.hts.fintech.flights.cfar.model.ConfirmContractRequest;
import com.hopper.hts.fintech.flights.cfar.model.ContactInfo;
import com.hopper.hts.fintech.flights.cfar.model.CreateDraftContractRequest;
import com.hopper.hts.fintech.flights.cfar.model.CreateOffersRequest;
import com.hopper.hts.fintech.flights.cfar.model.EmailAddress;
import com.hopper.hts.fintech.flights.cfar.model.ItineraryPassenger;
import com.hopper.hts.fintech.flights.cfar.model.ItinerarySlice;
import com.hopper.hts.fintech.flights.cfar.model.ItinerarySliceSegment;
import com.hopper.hts.fintech.flights.cfar.model.Literal;
import com.hopper.hts.fintech.flights.cfar.model.MerchandisingContext;
import com.hopper.hts.fintech.flights.cfar.model.MerchantOfRecord;
import com.hopper.hts.fintech.flights.cfar.model.OffersEligible;
import com.hopper.hts.fintech.flights.cfar.model.PassengerType;
import com.hopper.hts.fintech.flights.cfar.model.PaymentInformation;
import com.hopper.hts.fintech.flights.cfar.model.PreBooking;
import com.hopper.hts.fintech.flights.cfar.model.PriceOffersRequest;
import com.hopper.hts.fintech.flights.sdk.FlightsCfarClient;
import com.hopper.hts.fintech.flights.sdk.payments.CardDetails;
import com.hopper.hts.fintech.flights.sdk.payments.Payments;
import com.hopper.hts.fintech.flights.sdk.payments.SpreedlyTokenizer;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class CreateOfferAndContract {
    public static void main(String[] args) throws Exception {
        FlightsCfarClient hts = Env.htsClient();
        String sessionId = UUID.randomUUID().toString();

        AncillaryItinerary itinerary = sampleItinerary();
        OffersEligible offers = hts.offers().postOffers(offersRequest(sessionId, itinerary)).execute();
        if (offers == null || offers.getOffers().isEmpty()) {
            System.out.println("No CFAR offer available for this itinerary");
            return;
        }
        CfarOffer unpriced = offers.getOffers().get(0);
        System.out.printf("Offer %s %s, payout %s%%, priced=%s%n",
            unpriced.getPrice().getTotalAmount(), unpriced.getCurrency(), unpriced.getPayout().getPercentage(), unpriced.getPriced());

        OffersEligible priced = hts.offers().postOffersPrice(new PriceOffersRequest()
            .offerIds(Collections.singletonList(unpriced.getId()))
            .currency(unpriced.getCurrency())
            .itinerary(itinerary)
            .userId("user-42")).execute();
        CfarOffer offer = priced.getOffers().get(0);
        System.out.printf("Priced offer %s %s, priced=%s%n", offer.getPrice().getTotalAmount(), offer.getCurrency(), offer.getPriced());

        CfarContract draft = hts.contracts().postContracts(UUID.randomUUID(), contractRequest(offer)).execute();
        System.out.printf("Draft contract %s (%s)%n", draft.getId(), draft.getStatus());

        hts.contracts().putContractsIdConfirm(draft.getId(), new ConfirmContractRequest().bookingReferences(bookingReferences())).execute();
        CfarContract confirmed = hts.contracts().getContractsId(draft.getId()).execute();
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

    static CreateDraftContractRequest contractRequest(CfarOffer offer) throws Exception {
        String amount = offer.getPrice().getTotalAmount();
        SpreedlyTokenizer tokenizer = Env.spreedlyTokenizerOrNull();
        String presetToken = Env.optional("HTS_PAYMENT_TOKEN", "");

        MerchantOfRecord merchantOfRecord;
        PaymentInformation paymentInformation;
        if (!presetToken.isEmpty()) {
            merchantOfRecord = MerchantOfRecord.HTS;
            paymentInformation = Payments.provided(Payments.tokenizedCard(amount, presetToken));
        } else if (tokenizer == null) {
            merchantOfRecord = MerchantOfRecord.PARTNER;
            paymentInformation = Payments.notProvided(amount);
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
            paymentInformation = Payments.provided(Payments.tokenizedCard(amount, token));
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

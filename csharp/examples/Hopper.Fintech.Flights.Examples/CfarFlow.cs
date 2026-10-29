using Hopper.Fintech.Flights;
using Hopper.Fintech.Flights.Cfar.Model;

internal static class CfarFlow
{
    public static async Task RunAsync(HtsFlightsClient hts)
    {
        var itinerary = SampleItinerary();
        var offersResponse = await hts.Cfar.Offers.PostOffersAsync(new CreateOffersRequest(
            sessionId: Guid.NewGuid().ToString(),
            currency: "CAD",
            itinerary: itinerary,
            channel: new Channel(new ApiChannel("example-app", ApiChannel.TypeEnum.Api), "example-app"),
            pointOfSale: "CA",
            merchandisingContext: new MerchandisingContext(new PreBooking(PreBooking.TypeEnum.PreBooking))));
        if (!offersResponse.TryCreated(out var offers) || offers.Offers.Count == 0)
        {
            Console.WriteLine($"No CFAR offer available for this itinerary ({(int)offersResponse.StatusCode})");
            return;
        }
        var unpriced = offers.Offers[0];
        Console.WriteLine($"Offer {unpriced.Price.TotalAmount} {unpriced.Currency}, payout {unpriced.Payout.Percentage}%, priced={unpriced.Priced}");

        var priced = (await hts.Cfar.Offers.PostOffersPriceAsync(new PriceOffersRequest(
            offerIds: [unpriced.Id],
            currency: unpriced.Currency,
            itinerary: itinerary,
            userId: "user-42"))).Created()!;
        var offer = priced.Offers[0];
        Console.WriteLine($"Priced offer {offer.Price.TotalAmount} {offer.Currency}, priced={offer.Priced}");

        var (merchantOfRecord, paymentInformation) = await Payment(offer.Price.TotalAmount);
        var draftResponse = await hts.Cfar.Contracts.PostContractsAsync(Guid.NewGuid(), new CreateDraftContractRequest(
            offerId: offer.Id,
            currency: offer.Currency,
            locale: "en-CA",
            paymentInformation: paymentInformation,
            merchantOfRecord: merchantOfRecord,
            contactInfo: new ContactInfo(new EmailAddress(new Literal("ada@example.com", Literal.TypeEnum.Literal))),
            userId: "user-42"));
        var draft = draftResponse.Created() ?? throw new InvalidOperationException($"Draft failed: {(int)draftResponse.StatusCode} {draftResponse.RawContent}");
        Console.WriteLine($"Draft contract {draft.Id} ({draft.Status})");

        var confirm = await hts.Cfar.Contracts.PutContractsIdConfirmAsync(draft.Id, new ConfirmContractRequest(Env.BookingReferences()));
        if (!confirm.IsNoContent)
        {
            throw new InvalidOperationException($"Confirm failed: {(int)confirm.StatusCode} {confirm.RawContent}");
        }
        var confirmed = (await hts.Cfar.Contracts.GetContractsIdAsync(draft.Id)).Ok()!;
        Console.WriteLine($"Confirmed contract {confirmed.Id} ({confirmed.Status})");
    }

    private static async Task<(MerchantOfRecord, PaymentInformation)> Payment(string amount)
    {
        var presetToken = Env.Optional("HTS_PAYMENT_TOKEN", "");
        if (presetToken.Length > 0)
        {
            return (MerchantOfRecord.Hts, CfarPayments.Provided(CfarPayments.TokenizedCard(amount, presetToken)));
        }
        using var tokenizer = Env.SpreedlyTokenizerOrNull();
        if (tokenizer == null)
        {
            return (MerchantOfRecord.Partner, CfarPayments.NotProvided(amount));
        }
        var token = await tokenizer.TokenizeAsync(new CardDetails("Ada", "Lovelace", "4111111111111111", "12", "2030", "123"));
        Console.WriteLine($"Tokenized card with Spreedly: {token}");
        return (MerchantOfRecord.Hts, CfarPayments.Provided(CfarPayments.TokenizedCard(amount, token)));
    }

    private static AncillaryItinerary SampleItinerary() =>
        new(
            slices: [new ItinerarySlice([new ItinerarySliceSegment("YYZ", "YVR", "2026-12-10T08:00", "2026-12-10T10:15", Cabin.Economy, "WS", "701")], "econo")],
            ancillaries: [],
            passengers: [new ItineraryPassenger(PassengerType.Adult, [], "249.00")]);
}

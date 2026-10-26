using System.Diagnostics;
using System.Net;
using System.Text.Json;
using Hopper.Fintech.Flights.Cfar.Model;
using Xunit;

namespace Hopper.Fintech.Flights.Tests;

public class FlightsCfarClientTests
{
    private const string ContractId = "1f1ac8bb-c482-6ca6-9606-819367139ce3";

    private static FlightsCfarClient Client(RecordingHandler handler, HtsEnvironment? environment = null) =>
        FlightsCfarClient.Builder()
            .Environment(environment ?? HtsEnvironment.Staging)
            .ApiKey("test-key")
            .HttpMessageHandler(() => handler)
            .Build();

    [Fact]
    public async Task SendsRawApiKeyAndUserAgentToVersionedBasePath()
    {
        var handler = new RecordingHandler(HttpStatusCode.NoContent);
        using var client = Client(handler);

        await client.Contracts.PutContractsIdConfirmAsync(ContractId, new ConfirmContractRequest(new Dictionary<string, string> { ["pnr"] = "ABC123" }));

        var (request, body) = Assert.Single(handler.Requests);
        Assert.Equal(HttpMethod.Put, request.Method);
        Assert.Equal($"https://fintech.staging.htsapis.com/flights-cfar/v2/contracts/{ContractId}/confirm", request.RequestUri!.ToString());
        Assert.Equal("test-key", Assert.Single(request.Headers.GetValues("X-Api-Key")));
        Assert.StartsWith("hts-fintech-flights-csharp/", request.Headers.UserAgent.ToString());
        Assert.Contains("\"pnr\":\"ABC123\"", body);
    }

    [Fact]
    public async Task ProductionEnvironmentUsesProductionHost()
    {
        var handler = new RecordingHandler(HttpStatusCode.NoContent);
        using var client = Client(handler, HtsEnvironment.Production);

        await client.Contracts.PutContractsIdConfirmAsync(ContractId, new ConfirmContractRequest(new Dictionary<string, string>()));

        Assert.Equal("fintech.htsapis.com", Assert.Single(handler.Requests).Request.RequestUri!.Host);
    }

    [Fact]
    public async Task SendsIdempotencyKeyOnDraftContract()
    {
        var handler = new RecordingHandler(HttpStatusCode.BadRequest, "[]");
        using var client = Client(handler);
        var idempotencyKey = Guid.NewGuid();

        await client.Contracts.PostContractsAsync(idempotencyKey, DraftRequest());

        var request = Assert.Single(handler.Requests).Request;
        Assert.Equal(idempotencyKey.ToString(), Assert.Single(request.Headers.GetValues("Idempotency-Key")));
    }

    [Fact]
    public async Task NoContentOfferResponseMeansNotEligible()
    {
        var handler = new RecordingHandler(HttpStatusCode.NoContent);
        using var client = Client(handler);

        var response = await client.Offers.PostOffersAsync(OffersRequest());

        Assert.True(response.IsNoContent);
        Assert.False(response.TryCreated(out _));
    }

    [Fact]
    public async Task ConcurrentCallsAreNotThrottledBySingleApiKey()
    {
        var handler = new RecordingHandler(HttpStatusCode.NoContent);
        using var client = Client(handler);

        var stopwatch = Stopwatch.StartNew();
        await Task.WhenAll(Enumerable.Range(0, 200).Select(_ =>
            client.Contracts.PutContractsIdConfirmAsync(ContractId, new ConfirmContractRequest(new Dictionary<string, string>()))));
        stopwatch.Stop();

        Assert.Equal(200, handler.Requests.Count);
        Assert.True(stopwatch.Elapsed < TimeSpan.FromSeconds(2), $"200 calls took {stopwatch.Elapsed}");
    }


    [Fact]
    public void JsonSerializerOptionsRoundTripModelsInWireFormat()
    {
        using var client = Client(new RecordingHandler(HttpStatusCode.NoContent));
        var request = OffersRequest();

        var json = JsonSerializer.Serialize(request, client.JsonSerializerOptions);
        var parsed = JsonSerializer.Deserialize<CreateOffersRequest>(json, client.JsonSerializerOptions)!;

        Assert.Contains("\"session_id\"", json);
        Assert.Contains("\"type\":\"api\"", json);
        Assert.Equal(request.SessionId, parsed.SessionId);
        Assert.Equal("example-app", parsed.Channel.ApiChannel!.VarClient);
    }

    [Fact]
    public void BuildRequiresApiKeyAndEnvironment()
    {
        Assert.Throws<InvalidOperationException>(() => FlightsCfarClient.Builder().Environment(HtsEnvironment.Staging).Build());
        Assert.Throws<InvalidOperationException>(() => FlightsCfarClient.Builder().ApiKey("k").Build());
    }

    private static CreateOffersRequest OffersRequest()
    {
        var segment = new ItinerarySliceSegment("YYZ", "YVR", "2026-12-10T08:00", "2026-12-10T10:15", Cabin.Economy, "WS", "701");
        var itinerary = new AncillaryItinerary(
            [new ItinerarySlice([segment], "econo")],
            [],
            [new ItineraryPassenger(PassengerType.Adult, [], "249.00")]);
        return new CreateOffersRequest(
            Guid.NewGuid().ToString(),
            "CAD",
            itinerary,
            new Channel(new ApiChannel("example-app", ApiChannel.TypeEnum.Api), "example-app"),
            "CA",
            new MerchandisingContext(new PreBooking(PreBooking.TypeEnum.PreBooking)));
    }

    private static CreateDraftContractRequest DraftRequest() =>
        new(
            "offer-id",
            "CAD",
            "en-CA",
            new PaymentInformation(new NotProvided("10.00", NotProvided.TypeEnum.NotProvided)),
            MerchantOfRecord.Partner,
            new ContactInfo(new EmailAddress(new Literal("ada@example.com", Literal.TypeEnum.Literal))));
}

using System.Diagnostics;
using System.Net;
using System.Text.Json;
using Hopper.Fintech.Flights.Cfar.Model;
using Xunit;

namespace Hopper.Fintech.Flights.Tests;

public class HtsFlightsClientTests
{
    private const string ContractId = "1f1ac8bb-c482-6ca6-9606-819367139ce3";

    private static HtsFlightsClient Client(RecordingHandler handler, HtsEnvironment? environment = null) =>
        HtsFlightsClient.Builder()
            .Environment(environment ?? HtsEnvironment.Staging)
            .ApiKey("test-key")
            .HttpMessageHandler(() => handler)
            .Build();

    [Fact]
    public async Task SendsRawApiKeyAndUserAgentToVersionedBasePath()
    {
        var handler = new RecordingHandler(HttpStatusCode.NoContent);
        using var client = Client(handler);

        await client.Cfar.Contracts.PutContractsIdConfirmAsync(ContractId, new ConfirmContractRequest(new Dictionary<string, string> { ["pnr"] = "ABC123" }));

        var (request, body) = Assert.Single(handler.Requests);
        Assert.Equal(HttpMethod.Put, request.Method);
        Assert.Equal($"https://fintech.staging.htsapis.com/flights-cfar/v2/contracts/{ContractId}/confirm", request.RequestUri!.ToString());
        Assert.Equal("test-key", Assert.Single(request.Headers.GetValues("X-Api-Key")));
        Assert.StartsWith("hts-fintech-flights-csharp/", request.Headers.UserAgent.ToString());
        Assert.Contains("\"pnr\":\"ABC123\"", body);
    }

    [Fact]
    public async Task SendsDgCallsToDgBasePathWithSameApiKey()
    {
        var handler = new RecordingHandler(HttpStatusCode.NoContent);
        using var client = Client(handler);

        await client.Dg.Contracts.PutContractsIdConfirmAsync(ContractId, new Dg.Model.ConfirmContractRequest(new Dictionary<string, string> { ["pnr"] = "ABC123" }));

        var request = Assert.Single(handler.Requests).Request;
        Assert.Equal($"https://fintech.staging.htsapis.com/flights-dafar/v2/contracts/{ContractId}/confirm", request.RequestUri!.ToString());
        Assert.Equal("test-key", Assert.Single(request.Headers.GetValues("X-Api-Key")));
        Assert.StartsWith("hts-fintech-flights-csharp/", request.Headers.UserAgent.ToString());
    }

    [Fact]
    public async Task ProductBaseUrlsOverrideEnvironmentIndependently()
    {
        var handler = new RecordingHandler(HttpStatusCode.NoContent);
        using var client = HtsFlightsClient.Builder()
            .Environment(HtsEnvironment.Staging)
            .DgBaseUrl(new Uri("http://localhost:8080/flights-dafar/v2/"))
            .ApiKey("test-key")
            .HttpMessageHandler(() => handler)
            .Build();

        await client.Cfar.Contracts.PutContractsIdConfirmAsync(ContractId, new ConfirmContractRequest(new Dictionary<string, string>()));
        await client.Dg.Contracts.PutContractsIdConfirmAsync(ContractId, new Dg.Model.ConfirmContractRequest(new Dictionary<string, string>()));

        var uris = handler.Requests.Select(r => r.Request.RequestUri!.ToString()).ToList();
        Assert.Equal($"https://fintech.staging.htsapis.com/flights-cfar/v2/contracts/{ContractId}/confirm", uris[0]);
        Assert.Equal($"http://localhost:8080/flights-dafar/v2/contracts/{ContractId}/confirm", uris[1]);
    }

    [Fact]
    public async Task ProductionEnvironmentUsesProductionHost()
    {
        var handler = new RecordingHandler(HttpStatusCode.NoContent);
        using var client = Client(handler, HtsEnvironment.Production);

        await client.Cfar.Contracts.PutContractsIdConfirmAsync(ContractId, new ConfirmContractRequest(new Dictionary<string, string>()));

        Assert.Equal("fintech.htsapis.com", Assert.Single(handler.Requests).Request.RequestUri!.Host);
    }

    [Fact]
    public async Task SendsIdempotencyKeyOnDraftContract()
    {
        var handler = new RecordingHandler(HttpStatusCode.BadRequest, "[]");
        using var client = Client(handler);
        var idempotencyKey = Guid.NewGuid();

        await client.Cfar.Contracts.PostContractsAsync(idempotencyKey, DraftRequest());

        var request = Assert.Single(handler.Requests).Request;
        Assert.Equal(idempotencyKey.ToString(), Assert.Single(request.Headers.GetValues("Idempotency-Key")));
    }

    [Fact]
    public async Task NoContentOfferResponseMeansNotEligible()
    {
        var handler = new RecordingHandler(HttpStatusCode.NoContent);
        using var client = Client(handler);

        var response = await client.Cfar.Offers.PostOffersAsync(OffersRequest());

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
            client.Cfar.Contracts.PutContractsIdConfirmAsync(ContractId, new ConfirmContractRequest(new Dictionary<string, string>()))));
        stopwatch.Stop();

        Assert.Equal(200, handler.Requests.Count);
        Assert.True(stopwatch.Elapsed < TimeSpan.FromSeconds(2), $"200 calls took {stopwatch.Elapsed}");
    }


    [Fact]
    public void JsonSerializerOptionsRoundTripModelsInWireFormat()
    {
        using var client = Client(new RecordingHandler(HttpStatusCode.NoContent));
        var request = OffersRequest();

        var json = JsonSerializer.Serialize(request, client.Cfar.JsonSerializerOptions);
        var parsed = JsonSerializer.Deserialize<CreateOffersRequest>(json, client.Cfar.JsonSerializerOptions)!;

        Assert.Contains("\"session_id\"", json);
        Assert.Contains("\"type\":\"api\"", json);
        Assert.Equal(request.SessionId, parsed.SessionId);
        Assert.Equal("example-app", parsed.Channel.ApiChannel!.VarClient);
    }

    [Fact]
    public void BuildRequiresApiKeyAndEnvironment()
    {
        Assert.Throws<InvalidOperationException>(() => HtsFlightsClient.Builder().Environment(HtsEnvironment.Staging).Build());
        Assert.Throws<InvalidOperationException>(() => HtsFlightsClient.Builder().ApiKey("k").Build());
        Assert.Throws<InvalidOperationException>(() => HtsFlightsClient.Builder().ApiKey("k").CfarBaseUrl(new Uri("http://localhost/")).Build());
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

using System.Net;
using System.Text.Json;
using Xunit;

namespace Hopper.Fintech.Flights.Tests;

public class PaymentsTests
{
    [Fact]
    public void SerializesTokenizedCardAsProvidedPaymentInformation()
    {
        using var client = FlightsCfarClient.Builder()
            .Environment(HtsEnvironment.Staging)
            .ApiKey("k")
            .HttpMessageHandler(() => new RecordingHandler(HttpStatusCode.NoContent))
            .Build();

        var json = JsonDocument.Parse(JsonSerializer.Serialize(Payments.Provided(Payments.TokenizedCard("123.45", "pm_abc")), client.JsonSerializerOptions)).RootElement;

        Assert.Equal("provided", json.GetProperty("type").GetString());
        var card = json.GetProperty("payment_methods")[0];
        Assert.Equal("payment_card", card.GetProperty("type").GetString());
        Assert.Equal("123.45", card.GetProperty("amount").GetString());
        Assert.Equal("token", card.GetProperty("identifier").GetProperty("type").GetString());
        Assert.Equal("pm_abc", card.GetProperty("identifier").GetProperty("value").GetString());
    }
}

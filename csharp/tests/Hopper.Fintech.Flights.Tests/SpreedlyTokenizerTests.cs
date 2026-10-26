using System.Net;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using Xunit;

namespace Hopper.Fintech.Flights.Tests;

public class SpreedlyTokenizerTests
{
    private const string TokenResponse = """{"transaction":{"token":"txn_1","payment_method":{"token":"pm_123"}}}""";

    private static readonly CardDetails Card = new("Ada", "Lovelace", "4111111111111111", "12", "2030", "123");

    private static SpreedlyTokenizerBuilder Tokenizer(RecordingHandler handler) =>
        SpreedlyTokenizer.Builder()
            .EnvironmentKey("env")
            .AccessSecret("secret")
            .HttpMessageHandler(() => handler);

    [Fact]
    public async Task TokenizesWithBasicAuthAndReturnsPaymentMethodToken()
    {
        var handler = new RecordingHandler(HttpStatusCode.Created, TokenResponse);
        using var tokenizer = Tokenizer(handler).Build();

        var token = await tokenizer.TokenizeAsync(Card);

        Assert.Equal("pm_123", token);
        var (request, body) = Assert.Single(handler.Requests);
        Assert.Equal("https://core.spreedly.com/v1/payment_methods", request.RequestUri!.ToString());
        Assert.Equal("Basic " + Convert.ToBase64String(Encoding.UTF8.GetBytes("env:secret")), request.Headers.Authorization!.ToString());
        var paymentMethod = PaymentMethod(body);
        var creditCard = paymentMethod.GetProperty("credit_card");
        Assert.Equal("4111111111111111", creditCard.GetProperty("number").GetString());
        Assert.Equal("123", creditCard.GetProperty("verification_value").GetString());
        Assert.False(paymentMethod.TryGetProperty("encrypted_fields", out _));
    }

    [Fact]
    public async Task EncryptsCardFieldsWhenEncryptionIsConfigured()
    {
        using var rsa = RSA.Create(2048);
        var handler = new RecordingHandler(HttpStatusCode.Created, TokenResponse);
        using var tokenizer = Tokenizer(handler)
            .Encryption("cert_abc", Convert.ToBase64String(rsa.ExportSubjectPublicKeyInfo()))
            .Build();

        await tokenizer.TokenizeAsync(Card);

        var paymentMethod = PaymentMethod(Assert.Single(handler.Requests).Body);
        Assert.Equal("cert_abc", paymentMethod.GetProperty("encryption_certificate_token").GetString());
        Assert.Equal("number,month,year,first_name,last_name", paymentMethod.GetProperty("encrypted_fields").GetString());
        var creditCard = paymentMethod.GetProperty("credit_card");
        Assert.Equal("4111111111111111", Decrypt(rsa, creditCard.GetProperty("number").GetString()!));
        Assert.Equal("Ada", Decrypt(rsa, creditCard.GetProperty("first_name").GetString()!));
        Assert.Equal("2030", Decrypt(rsa, creditCard.GetProperty("year").GetString()!));
        Assert.Equal("123", creditCard.GetProperty("verification_value").GetString());
    }

    [Fact]
    public async Task ThrowsSpreedlyExceptionWithErrorDetailsOnUnprocessableCard()
    {
        var handler = new RecordingHandler(
            HttpStatusCode.UnprocessableEntity,
            """{"error":{"attribute":"number","key":"errors.invalid","message":"Number is invalid"}}""");
        using var tokenizer = Tokenizer(handler).Build();

        var error = await Assert.ThrowsAsync<SpreedlyException>(() => tokenizer.TokenizeAsync(Card));

        Assert.Equal(HttpStatusCode.UnprocessableEntity, error.StatusCode);
        Assert.Equal("number", error.Attribute);
        Assert.Equal("Number is invalid", error.Message);
    }

    [Fact]
    public void CardDetailsToStringHidesCardData()
    {
        Assert.Equal("CardDetails { LastFourDigits = 1111 }", Card.ToString());
    }

    [Fact]
    public void BuildRequiresCredentialsAndCompleteEncryption()
    {
        Assert.Throws<InvalidOperationException>(() => SpreedlyTokenizer.Builder().AccessSecret("s").Build());
        Assert.Throws<InvalidOperationException>(() => SpreedlyTokenizer.Builder().EnvironmentKey("e").Build());
        Assert.Throws<InvalidOperationException>(() => SpreedlyTokenizer.Builder().EnvironmentKey("e").AccessSecret("s").Encryption("cert", "").Build());
    }

    private static JsonElement PaymentMethod(string body) =>
        JsonDocument.Parse(body).RootElement.GetProperty("payment_method");

    private static string Decrypt(RSA rsa, string base64) =>
        Encoding.UTF8.GetString(rsa.Decrypt(Convert.FromBase64String(base64), RSAEncryptionPadding.Pkcs1));
}

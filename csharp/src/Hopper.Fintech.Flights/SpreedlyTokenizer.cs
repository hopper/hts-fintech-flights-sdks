using Hopper.Fintech.Flights.Spreedly.Api;
using Hopper.Fintech.Flights.Spreedly.Client;
using Hopper.Fintech.Flights.Spreedly.Extensions;
using Hopper.Fintech.Flights.Spreedly.Model;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Logging;

namespace Hopper.Fintech.Flights;

public sealed class SpreedlyTokenizer : IDisposable
{
    private const string EncryptedFields = "number,month,year,first_name,last_name";

    private readonly ServiceProvider _services;
    private readonly IPaymentApi _paymentApi;
    private readonly string? _encryptionCertificateToken;
    private readonly RsaEncryption? _encryption;

    private SpreedlyTokenizer(ServiceProvider services, string? encryptionCertificateToken, RsaEncryption? encryption)
    {
        _services = services;
        _paymentApi = services.GetRequiredService<IPaymentApi>();
        _encryptionCertificateToken = encryptionCertificateToken;
        _encryption = encryption;
    }

    public static SpreedlyTokenizerBuilder Builder() => new();

    public async Task<string> TokenizeAsync(CardDetails card, CancellationToken cancellationToken = default)
    {
        var response = await _paymentApi.PostPaymentMethodAsync(BuildRequest(card), cancellationToken).ConfigureAwait(false);
        if (response.TryCreated(out var created))
        {
            return created.Transaction.PaymentMethod.Token;
        }
        var error = response.IsUnprocessableContent ? response.UnprocessableContent()?.Error : null;
        throw new SpreedlyException(
            response.StatusCode,
            error?.Attribute,
            error?.Key,
            error?.Message ?? $"Spreedly tokenization failed with status {(int)response.StatusCode}");
    }

    public void Dispose()
    {
        _encryption?.Dispose();
        _services.Dispose();
    }

    internal CreatePaymentMethodRequest BuildRequest(CardDetails card)
    {
        var creditCard = new CreateCreditCard(
            firstName: Protect(card.FirstName),
            lastName: Protect(card.LastName),
            number: Protect(card.Number),
            verificationValue: card.VerificationValue,
            month: Protect(card.ExpirationMonth),
            year: Protect(card.ExpirationYear));
        var paymentMethod = _encryption == null
            ? new CreatePaymentMethod(creditCard)
            : new CreatePaymentMethod(creditCard, EncryptedFields, _encryptionCertificateToken!);
        return new CreatePaymentMethodRequest(paymentMethod);
    }

    private string Protect(string value) => _encryption?.Encrypt(value) ?? value;

    internal static SpreedlyTokenizer Build(
        Uri baseUrl,
        string environmentKey,
        string accessSecret,
        string? encryptionCertificateToken,
        string? encryptionPublicKey,
        TimeSpan timeout,
        ILoggerFactory? loggerFactory,
        Func<HttpMessageHandler>? handlerFactory)
    {
        var encryption = encryptionPublicKey == null ? null : new RsaEncryption(encryptionPublicKey);
        var services = new ServiceCollection();
        services.AddLogging();
        if (loggerFactory != null)
        {
            services.AddSingleton(loggerFactory);
        }

        services.AddApi(options =>
        {
            options.AddTokens(new BasicToken(environmentKey, accessSecret));
            options.UseProvider<StaticBasicTokenProvider, BasicToken>();
            options.AddApiHttpClients(
                client =>
                {
                    client.BaseAddress = baseUrl;
                    client.Timeout = timeout;
                    client.DefaultRequestHeaders.UserAgent.ParseAdd(SdkVersion.UserAgent);
                },
                builder =>
                {
                    if (handlerFactory != null)
                    {
                        builder.ConfigurePrimaryHttpMessageHandler(handlerFactory);
                    }
                });
        });

        return new SpreedlyTokenizer(services.BuildServiceProvider(), encryptionCertificateToken, encryption);
    }
}

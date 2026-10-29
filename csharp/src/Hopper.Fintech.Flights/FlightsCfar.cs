using Hopper.Fintech.Flights.Cfar.Api;
using Hopper.Fintech.Flights.Cfar.Client;
using Hopper.Fintech.Flights.Cfar.Extensions;
using System.Text.Json;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Logging;

namespace Hopper.Fintech.Flights;

public sealed class FlightsCfar : IDisposable
{
    private readonly ServiceProvider _services;

    private FlightsCfar(ServiceProvider services)
    {
        _services = services;
    }

    public IAnalyticsApi Analytics => _services.GetRequiredService<IAnalyticsApi>();

    public IContractManagementPostPurchaseApi Contracts => _services.GetRequiredService<IContractManagementPostPurchaseApi>();

    public IHTSSelfServiceExerciseApi HtsSelfServiceExercise => _services.GetRequiredService<IHTSSelfServiceExerciseApi>();

    public IOfferMerchandisingPrePurchaseApi Offers => _services.GetRequiredService<IOfferMerchandisingPrePurchaseApi>();

    public IPartnerManualExerciseApi PartnerManualExercise => _services.GetRequiredService<IPartnerManualExerciseApi>();

    public IPartnerSelfServiceExerciseApi PartnerSelfServiceExercise => _services.GetRequiredService<IPartnerSelfServiceExerciseApi>();

    public JsonSerializerOptions JsonSerializerOptions => _services.GetRequiredService<JsonSerializerOptionsProvider>().Options;

    public void Dispose() => _services.Dispose();

    internal static FlightsCfar Build(
        Uri baseUrl,
        string apiKey,
        TimeSpan timeout,
        ILoggerFactory? loggerFactory,
        IReadOnlyDictionary<string, string> defaultHeaders,
        Func<HttpMessageHandler>? handlerFactory)
    {
        var services = new ServiceCollection();
        services.AddLogging();
        if (loggerFactory != null)
        {
            services.AddSingleton(loggerFactory);
        }

        services.AddApi(options =>
        {
            options.AddTokens(new ApiKeyToken(apiKey, ClientUtils.ApiKeyHeader.X_Api_Key, prefix: string.Empty));
            options.UseProvider<CfarApiKeyProvider, ApiKeyToken>();
            options.AddApiHttpClients(
                client =>
                {
                    client.BaseAddress = baseUrl;
                    client.Timeout = timeout;
                    client.DefaultRequestHeaders.UserAgent.ParseAdd(SdkVersion.UserAgent);
                    foreach (var header in defaultHeaders)
                    {
                        client.DefaultRequestHeaders.TryAddWithoutValidation(header.Key, header.Value);
                    }
                },
                builder =>
                {
                    if (handlerFactory != null)
                    {
                        builder.ConfigurePrimaryHttpMessageHandler(handlerFactory);
                    }
                });
        });

        return new FlightsCfar(services.BuildServiceProvider());
    }
}

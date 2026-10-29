using Microsoft.Extensions.Logging;

namespace Hopper.Fintech.Flights;

public sealed class HtsFlightsClientBuilder
{
    private readonly Dictionary<string, string> _defaultHeaders = new(StringComparer.OrdinalIgnoreCase);
    private HtsEnvironment? _environment;
    private Uri? _cfarBaseUrl;
    private Uri? _dgBaseUrl;
    private string? _apiKey;
    private TimeSpan _timeout = TimeSpan.FromSeconds(60);
    private ILoggerFactory? _loggerFactory;
    private Func<HttpMessageHandler>? _handlerFactory;

    internal HtsFlightsClientBuilder()
    {
    }

    public HtsFlightsClientBuilder Environment(HtsEnvironment environment)
    {
        _environment = environment;
        return this;
    }

    public HtsFlightsClientBuilder CfarBaseUrl(Uri baseUrl)
    {
        _cfarBaseUrl = baseUrl;
        return this;
    }

    public HtsFlightsClientBuilder DgBaseUrl(Uri baseUrl)
    {
        _dgBaseUrl = baseUrl;
        return this;
    }

    public HtsFlightsClientBuilder ApiKey(string apiKey)
    {
        _apiKey = apiKey;
        return this;
    }

    public HtsFlightsClientBuilder Timeout(TimeSpan timeout)
    {
        _timeout = timeout;
        return this;
    }

    public HtsFlightsClientBuilder LoggerFactory(ILoggerFactory loggerFactory)
    {
        _loggerFactory = loggerFactory;
        return this;
    }

    public HtsFlightsClientBuilder DefaultHeader(string name, string value)
    {
        _defaultHeaders[name] = value;
        return this;
    }

    public HtsFlightsClientBuilder HttpMessageHandler(Func<HttpMessageHandler> handlerFactory)
    {
        _handlerFactory = handlerFactory;
        return this;
    }

    public HtsFlightsClient Build()
    {
        if (string.IsNullOrEmpty(_apiKey))
        {
            throw new InvalidOperationException("ApiKey is required");
        }
        var cfarBaseUrl = _cfarBaseUrl ?? _environment?.FlightsCfarBaseUrl
            ?? throw new InvalidOperationException("Environment or CfarBaseUrl is required");
        var dgBaseUrl = _dgBaseUrl ?? _environment?.FlightsDgBaseUrl
            ?? throw new InvalidOperationException("Environment or DgBaseUrl is required");
        return new HtsFlightsClient
        {
            Cfar = FlightsCfar.Build(cfarBaseUrl, _apiKey, _timeout, _loggerFactory, _defaultHeaders, _handlerFactory),
            Dg = FlightsDg.Build(dgBaseUrl, _apiKey, _timeout, _loggerFactory, _defaultHeaders, _handlerFactory),
        };
    }
}

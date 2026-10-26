using Microsoft.Extensions.Logging;

namespace Hopper.Fintech.Flights;

public sealed class FlightsCfarClientBuilder
{
    private readonly Dictionary<string, string> _defaultHeaders = new(StringComparer.OrdinalIgnoreCase);
    private Uri? _baseUrl;
    private string? _apiKey;
    private TimeSpan _timeout = TimeSpan.FromSeconds(60);
    private ILoggerFactory? _loggerFactory;
    private Func<HttpMessageHandler>? _handlerFactory;

    internal FlightsCfarClientBuilder()
    {
    }

    public FlightsCfarClientBuilder Environment(HtsEnvironment environment)
    {
        _baseUrl = environment.FlightsCfarBaseUrl;
        return this;
    }

    public FlightsCfarClientBuilder BaseUrl(Uri baseUrl)
    {
        _baseUrl = baseUrl;
        return this;
    }

    public FlightsCfarClientBuilder ApiKey(string apiKey)
    {
        _apiKey = apiKey;
        return this;
    }

    public FlightsCfarClientBuilder Timeout(TimeSpan timeout)
    {
        _timeout = timeout;
        return this;
    }

    public FlightsCfarClientBuilder LoggerFactory(ILoggerFactory loggerFactory)
    {
        _loggerFactory = loggerFactory;
        return this;
    }

    public FlightsCfarClientBuilder DefaultHeader(string name, string value)
    {
        _defaultHeaders[name] = value;
        return this;
    }

    public FlightsCfarClientBuilder HttpMessageHandler(Func<HttpMessageHandler> handlerFactory)
    {
        _handlerFactory = handlerFactory;
        return this;
    }

    public FlightsCfarClient Build()
    {
        if (_baseUrl == null)
        {
            throw new InvalidOperationException("BaseUrl or Environment is required");
        }
        if (string.IsNullOrEmpty(_apiKey))
        {
            throw new InvalidOperationException("ApiKey is required");
        }
        return FlightsCfarClient.Build(_baseUrl, _apiKey, _timeout, _loggerFactory, _defaultHeaders, _handlerFactory);
    }
}

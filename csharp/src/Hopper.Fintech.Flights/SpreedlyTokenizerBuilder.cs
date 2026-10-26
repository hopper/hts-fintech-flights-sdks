using Microsoft.Extensions.Logging;

namespace Hopper.Fintech.Flights;

public sealed class SpreedlyTokenizerBuilder
{
    private Uri _baseUrl = new("https://core.spreedly.com/v1");
    private string? _environmentKey;
    private string? _accessSecret;
    private string? _encryptionCertificateToken;
    private string? _encryptionPublicKey;
    private TimeSpan _timeout = TimeSpan.FromSeconds(60);
    private ILoggerFactory? _loggerFactory;
    private Func<HttpMessageHandler>? _handlerFactory;

    internal SpreedlyTokenizerBuilder()
    {
    }

    public SpreedlyTokenizerBuilder BaseUrl(Uri baseUrl)
    {
        _baseUrl = baseUrl;
        return this;
    }

    public SpreedlyTokenizerBuilder EnvironmentKey(string environmentKey)
    {
        _environmentKey = environmentKey;
        return this;
    }

    public SpreedlyTokenizerBuilder AccessSecret(string accessSecret)
    {
        _accessSecret = accessSecret;
        return this;
    }

    public SpreedlyTokenizerBuilder Encryption(string certificateToken, string base64PublicKey)
    {
        _encryptionCertificateToken = certificateToken;
        _encryptionPublicKey = base64PublicKey;
        return this;
    }

    public SpreedlyTokenizerBuilder Timeout(TimeSpan timeout)
    {
        _timeout = timeout;
        return this;
    }

    public SpreedlyTokenizerBuilder LoggerFactory(ILoggerFactory loggerFactory)
    {
        _loggerFactory = loggerFactory;
        return this;
    }

    public SpreedlyTokenizerBuilder HttpMessageHandler(Func<HttpMessageHandler> handlerFactory)
    {
        _handlerFactory = handlerFactory;
        return this;
    }

    public SpreedlyTokenizer Build()
    {
        if (string.IsNullOrEmpty(_environmentKey))
        {
            throw new InvalidOperationException("EnvironmentKey is required");
        }
        if (string.IsNullOrEmpty(_accessSecret))
        {
            throw new InvalidOperationException("AccessSecret is required");
        }
        if (string.IsNullOrEmpty(_encryptionCertificateToken) != string.IsNullOrEmpty(_encryptionPublicKey))
        {
            throw new InvalidOperationException("Encryption needs both a certificate token and a public key");
        }
        return SpreedlyTokenizer.Build(
            _baseUrl,
            _environmentKey,
            _accessSecret,
            _encryptionCertificateToken,
            _encryptionPublicKey,
            _timeout,
            _loggerFactory,
            _handlerFactory);
    }
}

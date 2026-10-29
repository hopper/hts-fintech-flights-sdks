using Hopper.Fintech.Flights.Cfar;
using Hopper.Fintech.Flights.Cfar.Client;

namespace Hopper.Fintech.Flights;

internal sealed class CfarApiKeyProvider : TokenProvider<ApiKeyToken>
{
    private readonly ApiKeyToken _token;

    public CfarApiKeyProvider(TokenContainer<ApiKeyToken> container)
    {
        _token = container.Tokens.Single();
    }

    protected internal override ValueTask<ApiKeyToken> GetAsync(string header = "", CancellationToken cancellation = default) =>
        ValueTask.FromResult(_token);
}

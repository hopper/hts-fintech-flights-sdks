using Hopper.Fintech.Flights.Dg;
using Hopper.Fintech.Flights.Dg.Client;

namespace Hopper.Fintech.Flights;

internal sealed class DgApiKeyProvider : TokenProvider<ApiKeyToken>
{
    private readonly ApiKeyToken _token;

    public DgApiKeyProvider(TokenContainer<ApiKeyToken> container)
    {
        _token = container.Tokens.Single();
    }

    protected internal override ValueTask<ApiKeyToken> GetAsync(string header = "", CancellationToken cancellation = default) =>
        ValueTask.FromResult(_token);
}

namespace Hopper.Fintech.Flights;

public sealed class HtsFlightsClient : IDisposable
{
    internal HtsFlightsClient()
    {
    }

    public static HtsFlightsClient Create(HtsEnvironment environment, string apiKey) =>
        Builder().Environment(environment).ApiKey(apiKey).Build();

    public static HtsFlightsClientBuilder Builder() => new();

    public FlightsCfar Cfar { get; internal init; } = null!;

    public FlightsDg Dg { get; internal init; } = null!;

    public void Dispose()
    {
        Cfar.Dispose();
        Dg.Dispose();
    }
}

namespace Hopper.Fintech.Flights;

public sealed class HtsEnvironment
{
    public static readonly HtsEnvironment Staging = new("https://fintech.staging.htsapis.com");
    public static readonly HtsEnvironment Production = new("https://fintech.htsapis.com");

    private HtsEnvironment(string host)
    {
        Host = host;
    }

    public string Host { get; }

    public Uri FlightsCfarBaseUrl => new(Host + "/flights-cfar/v2");
}

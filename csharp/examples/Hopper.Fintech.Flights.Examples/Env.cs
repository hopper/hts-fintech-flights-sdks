using Hopper.Fintech.Flights;

internal static class Env
{
    public static FlightsCfarClient HtsClient()
    {
        var builder = FlightsCfarClient.Builder().ApiKey(Optional("HTS_API_KEY", "unused"));
        var baseUrl = Optional("HTS_BASE_URL", "");
        if (baseUrl.Length > 0)
        {
            builder.BaseUrl(new Uri(baseUrl));
        }
        else
        {
            builder.Environment(Optional("HTS_ENV", "STAGING") == "PRODUCTION" ? HtsEnvironment.Production : HtsEnvironment.Staging);
            Required("HTS_API_KEY");
        }
        foreach (var (name, value) in Pairs(Optional("HTS_EXTRA_HEADERS", "")))
        {
            builder.DefaultHeader(name, value);
        }
        return builder.Build();
    }

    public static SpreedlyTokenizer? SpreedlyTokenizerOrNull()
    {
        var environmentKey = Optional("SPREEDLY_ENVIRONMENT_KEY", "");
        if (environmentKey.Length == 0)
        {
            return null;
        }
        var builder = SpreedlyTokenizer.Builder()
            .EnvironmentKey(environmentKey)
            .AccessSecret(Required("SPREEDLY_ACCESS_SECRET"));
        var certificateToken = Optional("SPREEDLY_CERTIFICATE_TOKEN", "");
        if (certificateToken.Length > 0)
        {
            builder.Encryption(certificateToken, Required("SPREEDLY_PUBLIC_KEY"));
        }
        return builder.Build();
    }

    public static Dictionary<string, string> BookingReferences() =>
        Pairs(Optional("HTS_BOOKING_REFERENCES", "pnr=ABC123")).ToDictionary(p => p.Name, p => p.Value);

    private static IEnumerable<(string Name, string Value)> Pairs(string csv) =>
        csv.Split(',', StringSplitOptions.RemoveEmptyEntries)
            .Select(pair => pair.Split('=', 2))
            .Where(parts => parts.Length == 2)
            .Select(parts => (parts[0].Trim(), parts[1].Trim()));

    private static string Required(string name)
    {
        var value = Environment.GetEnvironmentVariable(name);
        return string.IsNullOrEmpty(value) ? throw new InvalidOperationException($"Environment variable {name} is required") : value;
    }

    public static string Optional(string name, string fallback)
    {
        var value = Environment.GetEnvironmentVariable(name);
        return string.IsNullOrEmpty(value) ? fallback : value;
    }
}

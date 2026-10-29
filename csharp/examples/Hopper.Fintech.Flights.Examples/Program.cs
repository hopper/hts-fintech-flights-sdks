using Hopper.Fintech.Flights;

using var hts = Env.HtsClient();

switch (args.FirstOrDefault() ?? "cfar")
{
    case "cfar":
        await CfarFlow.RunAsync(hts);
        break;
    case "dg":
        await DgFlow.RunAsync(hts);
        break;
    default:
        Console.Error.WriteLine("usage: dotnet run -- [cfar|dg]");
        return 2;
}
return 0;

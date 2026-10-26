# C# SDK

C# SDK for the HTS Fintech Flights APIs (Cancel For Any Reason). It ships as one NuGet package, `Hopper.Fintech.Flights`, for .NET 10, and works with or without the .NET Generic Host. Every source file in the package is generated: the API clients from the published OpenAPI specifications, and the partner entry point from templates in `sdk/templates`.

## Layout

| Path | Contents | Maintained by |
| --- | --- | --- |
| `src/Hopper.Fintech.Flights/Cfar` | Client for the Flights CFAR API at `https://fintech.htsapis.com/flights-cfar/v2` (namespace `Hopper.Fintech.Flights.Cfar`) | generated from `../api/flights-cfar-v2.json` and `cfar/openapi-generator.json` |
| `src/Hopper.Fintech.Flights/Spreedly` | Internal client for Spreedly's payment method tokenization endpoint | generated from `../api/spreedly.yaml` and `spreedly/openapi-generator.json` |
| `src/Hopper.Fintech.Flights/*.cs` | Entry point for partners: `FlightsCfarClient`, `HtsEnvironment`, `SpreedlyTokenizer`, `CardDetails`, `Payments` | generated from `sdk/templates` and `sdk/openapi-generator.json` |
| `tests/Hopper.Fintech.Flights.Tests` | Unit tests for the entry point | hand-written |
| `examples/Hopper.Fintech.Flights.Examples` | Runnable walkthrough against staging | hand-written |

## Using the SDK

The Flights CFAR API is authenticated with an API key issued in the HTS developer portal. The SDK sends it in the `X-Api-Key` header on every request.

```csharp
using Hopper.Fintech.Flights;
using Hopper.Fintech.Flights.Cfar.Model;

using var hts = FlightsCfarClient.Create(HtsEnvironment.Production, Environment.GetEnvironmentVariable("HTS_API_KEY")!);

var offers = await hts.Offers.PostOffersAsync(createOffersRequest);
if (offers.TryCreated(out var eligible))
{
    var priced = (await hts.Offers.PostOffersPriceAsync(priceOffersRequest)).Created();
    var contract = (await hts.Contracts.PostContractsAsync(Guid.NewGuid(), createDraftContractRequest)).Created();
    await hts.Contracts.PutContractsIdConfirmAsync(contract!.Id, confirmContractRequest);
}
```

Create one `FlightsCfarClient` and reuse it for the life of the application; it owns its HTTP connections and is safe to call concurrently. Each property returns one of the generated API interfaces, grouped the same way as the API documentation. Every call returns a response object: check `IsCreated`, `IsNoContent`, `StatusCode`, or use `TryCreated(out ...)` / `Ok()` to read the body.

`POST /offers` answers `204 No Content` when the itinerary is not eligible, so `TryCreated` returns `false`. Offers come back unpriced. Price them with the same itinerary before drafting a contract, or the draft is rejected with `offer_not_priced`. Creating a contract needs an idempotency key, which the generated method takes as its first argument.

Pass `cancellationToken` by name (`cancellationToken: ct`). New optional parameters get default values, so existing calls keep compiling, but they are inserted before `cancellationToken`, so a positional token would not.

To log, cache, or replay a request or response yourself, serialize it with `hts.JsonSerializerOptions`. Plain `JsonSerializer.Serialize(model)` without those options produces the wrong JSON.

The builder also accepts a base URL, timeout, `ILoggerFactory`, extra default headers, and a custom `HttpMessageHandler`:

```csharp
using var hts = FlightsCfarClient.Builder()
    .Environment(HtsEnvironment.Staging)
    .ApiKey(apiKey)
    .Timeout(TimeSpan.FromSeconds(30))
    .LoggerFactory(loggerFactory)
    .Build();
```

## Card payments (HTS as merchant of record)

Card details never go to HTS. The SDK tokenizes them with Spreedly inside the partner's own process and only the resulting token is sent to HTS. Spreedly credentials and, where required, the encryption certificate are provided by HTS during onboarding.

```csharp
using var tokenizer = SpreedlyTokenizer.Builder()
    .EnvironmentKey(Environment.GetEnvironmentVariable("SPREEDLY_ENVIRONMENT_KEY")!)
    .AccessSecret(Environment.GetEnvironmentVariable("SPREEDLY_ACCESS_SECRET")!)
    .Encryption(Environment.GetEnvironmentVariable("SPREEDLY_CERTIFICATE_TOKEN")!, Environment.GetEnvironmentVariable("SPREEDLY_PUBLIC_KEY")!)
    .Build();

var token = await tokenizer.TokenizeAsync(new CardDetails("Ada", "Lovelace", "4111111111111111", "12", "2030", "123"));

var request = new CreateDraftContractRequest(
    offerId: offer.Id,
    currency: offer.Currency,
    locale: "en-CA",
    paymentInformation: Payments.Provided(Payments.TokenizedCard(offer.Price.TotalAmount, token)),
    merchantOfRecord: MerchantOfRecord.Hts,
    contactInfo: contactInfo,
    userId: userId);
```

With encryption configured, the card number, expiry, and cardholder name are RSA-encrypted before they leave the process. A rejected card throws `SpreedlyException` with Spreedly's status code, attribute, and message. `CardDetails.ToString()` shows only the last four digits, so a logged card does not leak its number.

When the partner is merchant of record, skip tokenization and send `Payments.NotProvided(amount)` with `MerchantOfRecord.Partner`.

## Examples

Run everything below from the `csharp/` directory. The example needs a staging API key from the HTS developer portal.

```bash
cp examples/.env.example examples/.env   # then fill in HTS_API_KEY
set -a; source examples/.env; set +a
dotnet run --project examples/Hopper.Fintech.Flights.Examples
```

It requests offers for a sample itinerary, prices the first one, drafts a contract, confirms it with the booking references in `HTS_BOOKING_REFERENCES`, and reads it back. With `SPREEDLY_ENVIRONMENT_KEY` set it tokenizes a test card and drafts the contract with HTS as merchant of record; `HTS_PAYMENT_TOKEN` uses an existing token instead. Without either, the partner is merchant of record.

## Regenerating

```bash
./generate.sh
```

The script uses the pinned OpenAPI Generator version, through Docker when available and otherwise by downloading the jar into `.generator/` (needs Java 11+). It runs three generations into `src/Hopper.Fintech.Flights`:

- `Cfar/` from `../api/flights-cfar-v2.json` and `cfar/openapi-generator.json`. Webhooks and partner-hosted endpoints are left out because partners receive those calls rather than make them.
- `Spreedly/` from `../api/spreedly.yaml` and `spreedly/openapi-generator.json`, with every type internal so partners only see `SpreedlyTokenizer`.
- The entry point and project file from `sdk/templates` and `sdk/openapi-generator.json`. `FlightsCfarClient` gets one property per API tag, named from `sdkAccessors` (a new tag without an entry falls back to its PascalCased name). `HtsEnvironment` takes its hosts from `sdkEnvironments` and its path from the spec. Only the files listed under `files` are copied into the project.

Never edit generated files. Change a spec, a generator config, or a template instead. Only the tests and examples are edited directly. CI fails if regenerating produces a diff.

## Building

```bash
dotnet build
dotnet test
dotnet pack -c Release
```

Requires the .NET 10 SDK; the package targets `net10.0`.

## Releasing

Releases are tagged `csharp-vX.Y.Z`. The tag's version is stamped into the package, which is built, tested, and pushed to NuGet as `Hopper.Fintech.Flights`, then attached to a GitHub release. Publishing uses NuGet trusted publishing, so no API key is stored in the repository.

Running the workflow by hand with a version is a dry run: it builds, tests, packs, and logs in to NuGet, but pushes nothing.

Locally, `dotnet pack -c Release -p:Version=0.0.0-local -o ./nupkg` produces the package for testing against a consuming project.

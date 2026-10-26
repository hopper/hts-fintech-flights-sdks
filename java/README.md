# Java SDK

Java SDK for the HTS Fintech Flights APIs: Cancel For Any Reason (CFAR) and Disruption Guarantee (DG). One client covers both products, so partners configure their API key and connection settings once. The API clients are generated from the published OpenAPI specification, so it always matches the live API. A small hand-written layer adds Spreedly card tokenization so card data never touches Hopper's servers.

## Modules

| Module | Contents | Maintained by |
| --- | --- | --- |
| `flights-cfar-client` | Client for the Flights CFAR API at `https://fintech.htsapis.com/flights-cfar/v2` | generated from `../api/flights-cfar-v2.json` |
| `flights-dafar-client` | Client for the Flights Disruption Guarantee API at `https://fintech.htsapis.com/flights-dafar/v2` | generated from `../api/flights-dafar-v2.json` |
| `spreedly-client` | Client for Spreedly's payment method tokenization endpoint | generated from `../api/spreedly.yaml` |
| `sdk` | Entry point for partners: one configured client for both products, plus card tokenization | hand-written |

Partners depend on the `sdk` artifact only. The client artifacts come with it.

## Using the SDK

Both APIs are authenticated with an API key issued in the HTS developer portal. The SDK sends it in the `X-Api-Key` header on every request. Each product is a separate registration in the portal, so register your application for every product you sell.

```java
HtsFlightsClient hts = HtsFlightsClient.create(HtsEnvironment.PRODUCTION, System.getenv("HTS_API_KEY"));

OffersEligible offers = hts.cfar().offers().postOffers(createOffersRequest).execute();
OffersEligible priced = hts.cfar().offers().postOffersPrice(priceOffersRequest).execute();
CfarContract contract = hts.cfar().contracts().postContracts(UUID.randomUUID(), createDraftContractRequest).execute();
hts.cfar().contracts().putContractsIdConfirm(contract.getId(), confirmContractRequest).execute();

DafarContract dg = hts.dg().contracts().getContractsId(contractId).execute();
```

`hts.cfar()` and `hts.dg()` have the same sections, named after the API documentation: `offers()`, `contracts()`, `partnerSelfServiceExercise()`, `partnerManualExercise()`, `htsSelfServiceExercise()` and `analytics()`. DG adds `flightStatusUpdates()`. Each returns a generated API class. Models live in `com.hopper.fintech.flights.cfar.model` and `com.hopper.fintech.flights.dafar.model`; the two products share many model names, so import from the product you are calling.

Timeouts, debugging and a custom `OkHttpClient` are set once on `HtsFlightsClient.builder()`, and both products share the same connection pool. `hts.cfar().apiClient()` and `hts.dg().apiClient()` expose the underlying generated clients, for example to add default headers.

Offers come back unpriced. Price them with the same itinerary before drafting a contract, or the draft is rejected with `offer_not_priced`. Each generated method takes the operation's required parameters and returns a request builder. Optional parameters are set on the builder, and `execute()` sends the request, so adding an optional parameter to the API does not change existing calls. Creating a contract needs an idempotency key, which the generated method takes as its first argument.

### Paying with a card

Card details never go to HTS. The SDK tokenizes them with Spreedly inside the partner's own process and only the resulting token is sent to HTS. Spreedly credentials and, where required, the encryption certificate are provided by HTS during onboarding.

```java
SpreedlyTokenizer tokenizer = SpreedlyTokenizer.builder()
    .environmentKey(System.getenv("SPREEDLY_ENVIRONMENT_KEY"))
    .accessSecret(System.getenv("SPREEDLY_ACCESS_SECRET"))
    .encryption(System.getenv("SPREEDLY_CERTIFICATE_TOKEN"), System.getenv("SPREEDLY_PUBLIC_KEY"))
    .build();

String token = tokenizer.tokenize(CardDetails.builder()
    .firstName("Ada").lastName("Lovelace")
    .number(cardNumber).expirationMonth("12").expirationYear("2030").verificationValue(cvv)
    .build());

CreateDraftContractRequest request = new CreateDraftContractRequest()
    .offerId(offer.getId())
    .currency("CAD")
    .locale("en-CA")
    .paymentInformation(CfarPayments.provided(CfarPayments.tokenizedCard(offer.getPremium(), token)))
    .contactInfo(contactInfo)
    .billingInfo(billingInfo);
```

For DG, build the same request from `com.hopper.fintech.flights.dafar.model` with `DgPayments`. The token is the same.

## Examples

Run everything below from the `java/` directory. The `examples` module holds runnable walkthroughs against staging. They need a staging API key from the HTS developer portal; the Spreedly variables are optional and switch the contract example from partner-as-merchant to HTS-as-merchant with a tokenized test card.

```bash
export HTS_API_KEY=...
export SPREEDLY_ENVIRONMENT_KEY=... SPREEDLY_ACCESS_SECRET=...            # optional
export SPREEDLY_CERTIFICATE_TOKEN=... SPREEDLY_PUBLIC_KEY=...              # optional, enables field encryption

./gradlew :examples:runTokenizeCard
./gradlew :examples:runCreateCfarContract
./gradlew :examples:runExerciseCfarContract -PexampleArgs="<contractId> self-service"
./gradlew :examples:runExerciseCfarContract -PexampleArgs="<contractId> url"
./gradlew :examples:runCreateDgContract
./gradlew :examples:runExerciseDgContract -PexampleArgs="<contractId> url"
./gradlew :examples:runExerciseDgContract -PexampleArgs="<contractId> eligibility"
./gradlew :examples:runExerciseDgContract -PexampleArgs="<contractId> refund"
```

`TokenizeCard` tokenizes a Spreedly test card and needs only the Spreedly variables. `CreateCfarContract` and `CreateDgContract` request an offer for a sample itinerary, creates a draft contract, and confirms it with the booking references in `HTS_BOOKING_REFERENCES` (comma-separated key=value pairs, default `pnr=ABC123`). The self-service exercise later fetches the booking from the partner using those references, so they must identify a booking the partner's system can return. Set `HTS_PAYMENT_TOKEN` to a Spreedly payment method token to run the HTS-as-merchant path with a token created elsewhere, for example in the browser with Spreedly's client-side tokenization, which needs only the environment key.

The token must come from the Spreedly environment HTS has configured for your tenant. A token from any other environment is rejected with `payment_authorization_failure`. `ExerciseCfarContract` either runs the partner self-service exercise (quote, then fulfill with the first payout option) or mints an HTS-hosted exercise URL to hand to the customer. `ExerciseDgContract` mints an HTS-hosted exercise URL, prints the contract's rebook eligibility, or takes a refund through an exercise quote. Set `HTS_ENV=PRODUCTION` to target production and `HTS_DEBUG=true` to log requests and responses. A template for the variables is in `examples/.env.example`.

The encryption certificate must be one Spreedly generated, so Spreedly holds the private key needed to decrypt the fields. A certificate created from a Hopper-supplied key pair, such as the ones used for the payment iframe, is the reverse direction and Spreedly answers with a bare 500.

## Regenerating the clients

```bash
./generate.sh
./gradlew build
```

`generate.sh` runs OpenAPI Generator 7.26.0 against the specs in the repository's top-level `api/` directory and writes the full output into each client module. CFAR and DG are generated separately, into their own packages, because the two specs use many of the same schema names with different shapes. It uses Docker when available and otherwise downloads the generator jar and runs it with the local Java, so either works; set `GENERATOR_NO_DOCKER=1` to force the jar. Every file under `src/main` in the client modules is generated and will be overwritten; do not edit them. Files the build owns (Gradle files, README, docs, tests) are excluded through each module's `.openapi-generator-ignore`.

Generation is idempotent: running it twice produces identical sources, so a spec change is the only thing that shows up in a diff.

### Generator settings that matter

- `disallowAdditionalPropertiesIfNotPresent=false` stops the generated models from rejecting fields they do not know about. Older SDK versions keep working when the API adds a field. Required-field and type checks are kept.
- `hideGenerationTimestamp=true` keeps regeneration diffs limited to real changes.
- The spec's webhooks are events HTS sends to the partner, but the generator would emit them as outbound API calls. `generate.sh` feeds the generator a copy of the spec without the `webhooks` section; the event models (`ContractConfirmed`, `ContractExercised`, and so on) are still generated from the schemas so partners can parse incoming webhooks.
- The generator never deletes files. If an endpoint or model is removed from the spec, its generated file lingers and has to be removed by hand.

## Updating the spec

`../api/flights-cfar-v2.json` and `../api/flights-dafar-v2.json` are the specs exactly as published on the HTS developer portal. When HTS publishes a new version, the spec and regenerated clients are updated here. To update by hand, download the spec from the portal into `../api/`, run `./generate.sh`, and commit the result.

## Releasing

Releases are tagged `java-vX.Y.Z`. Each release is built with that version, signed, and published to Maven Central.

Releases that don't break anything happen on their own. After every merge to `main` that touches `java/` or `api/`, CI compares the public Java API with the last release:

- **Nothing breaks** (new classes, methods or fields, or internal changes): CI bumps the patch version and publishes it.
- **Something breaks** (a removed or renamed class, method or field, or a changed signature): CI doesn't release. The run summary lists the breaking changes. To release anyway, run the "Auto-release Java SDK" workflow by hand with `allow_breaking` checked. While the SDK is below 1.0, that bumps the minor version; from 1.0 on, it bumps the major version.

To check a change locally before merging, run `./gradlew apiCompatibility -PbaselineVersion=<last release>` and read `*/build/japicmp/report.txt`.

Four artifacts are published under `com.hopper.cloud`:

| Artifact | Module |
| --- | --- |
| `hts-fintech-flights-sdk` | `sdk`, the one partners depend on |
| `hts-fintech-flights-cfar-client` | `flights-cfar-client` |
| `hts-fintech-flights-dafar-client` | `flights-dafar-client` |
| `hts-fintech-spreedly-client` | `spreedly-client` |

Locally, `./gradlew publishToMavenLocal -PsdkVersion=0.0.0-local` publishes unsigned artifacts to `~/.m2` for testing against a consuming project.

## Requirements

Java 11 or newer.

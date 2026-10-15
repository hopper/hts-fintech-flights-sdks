# Java SDK

Java SDK for the HTS Fintech Flights APIs (Cancel For Any Reason). The API client is generated from the published OpenAPI specification, so it always matches the live API. A small hand-written layer adds Spreedly card tokenization so card data never touches Hopper's servers.

## Modules

| Module | Contents | Maintained by |
| --- | --- | --- |
| `flights-cfar-client` | Client for the Flights CFAR API at `https://fintech.htsapis.com/flights-cfar/v2` | generated from `../api/flights-cfar-v2.json` |
| `spreedly-client` | Client for Spreedly's payment method tokenization endpoint | generated from `../api/spreedly.yaml` |
| `sdk` | Entry point for partners: wires the two clients together and handles card tokenization | hand-written |

Partners depend on the `sdk` artifact only. The two client artifacts come with it.

## Using the SDK

The Flights CFAR API is served through Kong and authenticated with an API key issued in the HTS developer portal. The SDK sends it in the `X-Api-Key` header on every request.

```java
FlightsCfarClient hts = FlightsCfarClient.create(HtsEnvironment.PRODUCTION, System.getenv("HTS_API_KEY"));

OffersEligible offers = hts.offers().postOffers(createOffersRequest).execute();
OffersEligible priced = hts.offers().postOffersPrice(priceOffersRequest).execute();
CfarContract contract = hts.contracts().postContracts(UUID.randomUUID(), createDraftContractRequest).execute();
hts.contracts().putContractsIdConfirm(contract.getId(), confirmContractRequest).execute();
```

Each accessor on `FlightsCfarClient` returns one of the generated API classes, grouped the same way as the API documentation. `hts.apiClient()` exposes the underlying generated client for timeouts, debugging, or custom headers.

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
    .paymentInformation(Payments.provided(Payments.tokenizedCard(offer.getPremium(), token)))
    .contactInfo(contactInfo)
    .billingInfo(billingInfo);
```

## Examples

Run everything below from the `java/` directory. The `examples` module holds runnable walkthroughs against staging. They need a staging API key from the HTS developer portal; the Spreedly variables are optional and switch the contract example from partner-as-merchant to HTS-as-merchant with a tokenized test card.

```bash
export HTS_API_KEY=...
export SPREEDLY_ENVIRONMENT_KEY=... SPREEDLY_ACCESS_SECRET=...            # optional
export SPREEDLY_CERTIFICATE_TOKEN=... SPREEDLY_PUBLIC_KEY=...              # optional, enables field encryption

./gradlew :examples:runTokenizeCard
./gradlew :examples:runCreateContract
./gradlew :examples:runExerciseContract -PexampleArgs="<contractId> self-service"
./gradlew :examples:runExerciseContract -PexampleArgs="<contractId> url"
```

`TokenizeCard` tokenizes a Spreedly test card and needs only the Spreedly variables. `CreateOfferAndContract` requests an offer for a sample itinerary, creates a draft contract, and confirms it with the booking references in `HTS_BOOKING_REFERENCES` (comma-separated key=value pairs, default `pnr=ABC123`). The self-service exercise later fetches the booking from the partner using those references, so they must identify a booking the partner's system can return. Set `HTS_PAYMENT_TOKEN` to a Spreedly payment method token to run the HTS-as-merchant path with a token created elsewhere, for example in the browser with Spreedly's client-side tokenization, which needs only the environment key.

The token must come from the Spreedly environment HTS has configured for your tenant. A token from any other environment is rejected with `payment_authorization_failure`. `ExerciseContract` either runs the partner self-service exercise (quote, then fulfill with the first payout option) or mints an HTS-hosted exercise URL to hand to the customer. Set `HTS_ENV=PRODUCTION` to target production and `HTS_DEBUG=true` to log requests and responses. A template for the variables is in `examples/.env.example`.

The encryption certificate must be one Spreedly generated, so Spreedly holds the private key needed to decrypt the fields. A certificate created from a Hopper-supplied key pair, such as the ones used for the payment iframe, is the reverse direction and Spreedly answers with a bare 500.

## Regenerating the clients

```bash
./generate.sh
./gradlew build
```

`generate.sh` runs OpenAPI Generator 7.26.0 against the specs in the repository's top-level `api/` directory and writes the full output into each client module. It uses Docker when available and otherwise downloads the generator jar and runs it with the local Java, so either works; set `GENERATOR_NO_DOCKER=1` to force the jar. Every file under `src/main` in the two client modules is generated and will be overwritten; do not edit them. Files the build owns (Gradle files, README, docs, tests) are excluded through each module's `.openapi-generator-ignore`.

Generation is idempotent: running it twice produces identical sources, so a spec change is the only thing that shows up in a diff.

### Generator settings that matter

- `disallowAdditionalPropertiesIfNotPresent=false` stops the generated models from rejecting fields they do not know about. Older SDK versions keep working when the API adds a field. Required-field and type checks are kept.
- `hideGenerationTimestamp=true` keeps regeneration diffs limited to real changes.
- The spec's webhooks are events HTS sends to the partner, but the generator would emit them as outbound API calls. `generate.sh` feeds the generator a copy of the spec without the `webhooks` section; the event models (`ContractConfirmed`, `ContractExercised`, and so on) are still generated from the schemas so partners can parse incoming webhooks.
- The generator never deletes files. If an endpoint or model is removed from the spec, its generated file lingers and has to be removed by hand.

## Updating the spec

`../api/flights-cfar-v2.json` is the spec exactly as published on the HTS developer portal. When HTS publishes a new version, the spec and regenerated clients are updated here. To update by hand, download the spec from the portal into `../api/`, run `./generate.sh`, and commit the result.

## Releasing

Releases are tagged `java-vX.Y.Z`. Each release is built with that version, signed, and published to Maven Central. Three artifacts are published under `com.hopper.cloud`:

| Artifact | Module |
| --- | --- |
| `hts-fintech-flights-sdk` | `sdk`, the one partners depend on |
| `hts-fintech-flights-cfar-client` | `flights-cfar-client` |
| `hts-fintech-spreedly-client` | `spreedly-client` |

Locally, `./gradlew publishToMavenLocal -PsdkVersion=0.0.0-local` publishes unsigned artifacts to `~/.m2` for testing against a consuming project.

## Requirements

Java 11 or newer.

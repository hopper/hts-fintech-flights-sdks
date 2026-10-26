namespace Hopper.Fintech.Flights;

public sealed record CardDetails(
    string FirstName,
    string LastName,
    string Number,
    string ExpirationMonth,
    string ExpirationYear,
    string VerificationValue)
{
    public string LastFourDigits => Number.Length <= 4 ? Number : Number[^4..];

    public override string ToString() => $"CardDetails {{ LastFourDigits = {LastFourDigits} }}";
}

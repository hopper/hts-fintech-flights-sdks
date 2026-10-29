using Hopper.Fintech.Flights.Dg.Model;

namespace Hopper.Fintech.Flights;

public static class DgPayments
{
    public static ProvidedPayment TokenizedCard(string amount, string token) =>
        new(new PaymentCard(amount, new Identifier(new Token(token, Token.TypeEnum.Token)), PaymentCard.TypeEnum.PaymentCard));

    public static PaymentInformation Provided(params ProvidedPayment[] paymentMethods) =>
        Provided((IEnumerable<ProvidedPayment>)paymentMethods);

    public static PaymentInformation Provided(IEnumerable<ProvidedPayment> paymentMethods) =>
        new(new Provided(paymentMethods.ToList(), Dg.Model.Provided.TypeEnum.Provided));

    public static PaymentInformation NotProvided(string amount) =>
        new(new NotProvided(amount, Dg.Model.NotProvided.TypeEnum.NotProvided));
}

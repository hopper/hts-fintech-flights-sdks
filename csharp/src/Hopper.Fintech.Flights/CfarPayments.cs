using Hopper.Fintech.Flights.Cfar.Model;

namespace Hopper.Fintech.Flights;

public static class CfarPayments
{
    public static ProvidedPayment TokenizedCard(string amount, string token) =>
        new(new PaymentCard(amount, new Identifier(new Token(token, Token.TypeEnum.Token)), PaymentCard.TypeEnum.PaymentCard));

    public static PaymentInformation Provided(params ProvidedPayment[] paymentMethods) =>
        Provided((IEnumerable<ProvidedPayment>)paymentMethods);

    public static PaymentInformation Provided(IEnumerable<ProvidedPayment> paymentMethods) =>
        new(new Provided(paymentMethods.ToList(), Cfar.Model.Provided.TypeEnum.Provided));

    public static PaymentInformation NotProvided(string amount) =>
        new(new NotProvided(amount, Cfar.Model.NotProvided.TypeEnum.NotProvided));
}

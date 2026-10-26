using System.Security.Cryptography;
using System.Text;

namespace Hopper.Fintech.Flights;

internal sealed class RsaEncryption : IDisposable
{
    private readonly RSA _rsa = RSA.Create();

    internal RsaEncryption(string base64PublicKey)
    {
        try
        {
            _rsa.ImportSubjectPublicKeyInfo(Convert.FromBase64String(base64PublicKey), out _);
        }
        catch (Exception e) when (e is FormatException or CryptographicException)
        {
            _rsa.Dispose();
            throw new ArgumentException("Invalid base64-encoded RSA public key", nameof(base64PublicKey), e);
        }
    }

    internal string Encrypt(string value) =>
        Convert.ToBase64String(_rsa.Encrypt(Encoding.UTF8.GetBytes(value), RSAEncryptionPadding.Pkcs1));

    public void Dispose() => _rsa.Dispose();
}

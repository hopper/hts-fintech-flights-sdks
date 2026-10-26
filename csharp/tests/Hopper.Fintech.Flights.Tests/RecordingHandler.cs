using System.Collections.Concurrent;
using System.Net;
using System.Text;

namespace Hopper.Fintech.Flights.Tests;

internal sealed class RecordingHandler : HttpMessageHandler
{
    private readonly HttpStatusCode _status;
    private readonly string _body;

    public RecordingHandler(HttpStatusCode status, string body = "")
    {
        _status = status;
        _body = body;
    }

    public ConcurrentQueue<(HttpRequestMessage Request, string Body)> Requests { get; } = new();

    protected override async Task<HttpResponseMessage> SendAsync(HttpRequestMessage request, CancellationToken cancellationToken)
    {
        var body = request.Content == null ? string.Empty : await request.Content.ReadAsStringAsync(cancellationToken);
        Requests.Enqueue((request, body));
        return new HttpResponseMessage(_status) { Content = new StringContent(_body, Encoding.UTF8, "application/json") };
    }
}

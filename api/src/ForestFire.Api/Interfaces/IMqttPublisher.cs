namespace ForestFire.Api.Interfaces
{
    public interface IMqttPublisher
    {
        // retain = true: brokeren gemmer den sidste besked på topic'et, så en klient der forbinder senere også får den
        Task PublishAsync(string topic, string payload, bool retain = false, CancellationToken stoppingToken = default);
    }
}

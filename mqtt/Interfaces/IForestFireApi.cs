using ForestFire.Mqtt.Models;

namespace ForestFire.Mqtt.Interfaces
{
    // HTTP-kaldene til ForestFire.Api
    public interface IForestFireApi
    {
        // POST /readings. Giver den gemte måling med risiko og isFire, eller null hvis API'et afviste den
        Task<Reading?> PostReadingAsync(SensorData data, CancellationToken ct);

        // PUT /firefighters/{id}
        Task PutFirefighterLocationAsync(FirefighterLocation location, CancellationToken ct);
    }
}

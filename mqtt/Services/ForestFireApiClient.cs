using System.Net;
using System.Net.Http.Json;
using ForestFire.Mqtt.Interfaces;
using ForestFire.Mqtt.Models;

namespace ForestFire.Mqtt.Services
{
    public class ForestFireApiClient : IForestFireApi
    {
        private readonly HttpClient http;
        private readonly string apiUrl;

        public ForestFireApiClient(HttpClient http, IConfiguration config)
        {
            this.http = http;
            apiUrl = config["Api:BaseUrl"]!.TrimEnd('/');
        }

        public async Task<Reading?> PostReadingAsync(SensorData data, CancellationToken ct)
        {
            // POST http://api:8080/readings med målingen som JSON
            using var response = await http.PostAsJsonAsync(apiUrl + "/readings", data, ct);

            // API'et afviste målingen. Den bliver ikke gyldig af at prøve igen
            if (response.StatusCode == HttpStatusCode.BadRequest)
                return null;

            // alt andet end 2xx: kast, så beskeden ikke ackes og prøves igen
            response.EnsureSuccessStatusCode();
            return await response.Content.ReadFromJsonAsync<Reading>(ct);
        }

        public async Task PutFirefighterLocationAsync(FirefighterLocation location, CancellationToken ct)
        {
            // PUT http://api:8080/firefighters/{id}
            using var response = await http.PutAsJsonAsync(
                apiUrl + "/firefighters/" + Uri.EscapeDataString(location.Id),
                new { lat = location.Lat, lon = location.Lon }, ct);
            response.EnsureSuccessStatusCode();
        }
    }
}

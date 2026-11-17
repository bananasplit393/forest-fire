namespace ForestFire.Mqtt.Models
{
    // SensorData fra coap-serveren på sensors/{nodeId}/data. Samme JSON sendes videre til POST /readings
    public class SensorData
    {
        public string? Node { get; set; }
        public long Ts { get; set; }
        public double? Temp { get; set; }
        public double? Humidity { get; set; }
        public double? Pm10 { get; set; }
        public double? Pm25 { get; set; }
        public double? Lat { get; set; }
        public double? Lon { get; set; }
    }
}

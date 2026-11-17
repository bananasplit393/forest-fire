namespace ForestFire.Mqtt.Models
{
    // FirefighterLocation fra appen på firefighters/{id}/location, hvert 10. sekund mens en alarm er aktiv
    public class FirefighterLocation
    {
        // brandmandens id, tages fra topic'et
        public string Id { get; set; } = string.Empty;
        public double Lat { get; set; }
        public double Lon { get; set; }
    }
}

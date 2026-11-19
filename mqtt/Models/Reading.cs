namespace ForestFire.Mqtt.Models
{
    // svaret fra POST /readings: den gemte måling med risikomodellens resultat
    public class Reading
    {
        public Guid Id { get; set; }
        public string? Node { get; set; }
        public long Ts { get; set; }
        public double? Temp { get; set; }
        public double? Humidity { get; set; }
        public double? Pm10 { get; set; }
        public double? Pm25 { get; set; }
        public double? Lat { get; set; }
        public double? Lon { get; set; }
        public double? FireProbability { get; set; }
        public bool? IsFire { get; set; }
    }
}

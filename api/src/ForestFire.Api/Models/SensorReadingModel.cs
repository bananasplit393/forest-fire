namespace ForestFire.Api.Models;

public class SensorReadingModel
{
    public Guid Id { get; set; }

    public DateTime Timestamp { get; set; }

    public double? Temp { get; set; }
    public double? Humidity { get; set; }  
    public double? Pm10 { get; set; }       
    public double? Pm25 { get; set; }      
    public double? Lat { get; set; }
    public double? Lon { get; set; }

    public double? FireProbability { get; set; }
    public bool? IsFire { get; set; }

    public DateTime CreatedAt { get; set; }
}

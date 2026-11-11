namespace ForestFire.Api.Models;
using System.ComponentModel.DataAnnotations;


public class CreateSensorReadingDto
{
    public long Ts { get; set; }

    public double? Temp { get; set; }
    public double? Humidity { get; set; }
    public double? Pm10 { get; set; }
    public double? Pm25 { get; set; }
    public double? Lat { get; set; }
    public double? Lon { get; set; }
}

public class SensorReadingDto
{
    public Guid Id { get; set; }
    public long Ts { get; set; }
    public DateTime Timestamp { get; set; }

    public double? Temp { get; set; }
    public double? Humidity { get; set; }
    public double? Pm10 { get; set; }
    public double? Pm25 { get; set; }
    public double? Lat { get; set; }
    public double? Lon { get; set; }


    // fra risikomodellen, null hvis den ikke kunne kÃ¸re
    public double? FireProbability { get; set; }
    public bool? IsFire { get; set; }

    public DateTime CreatedAt { get; set; }
}


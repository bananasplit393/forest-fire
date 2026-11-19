namespace ForestFire.Api.Models;

// seneste position for en brandmand. Kommer fra appen på firefighters/{id}/location via mqtt-servicen
public class FirefighterLocation
{
    public string Id { get; set; } = string.Empty;

    public double Lat { get; set; }
    public double Lon { get; set; }

    // hvornår backenden modtog positionen
    public DateTime ReceivedAt { get; set; }
}

// body på PUT /firefighters/{id}
public class FirefighterLocationDto
{
    public double Lat { get; set; }
    public double Lon { get; set; }
}

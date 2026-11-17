using ForestFire.Api.Models;
using ForestFire.Api.Services;
using Microsoft.AspNetCore.Mvc;

namespace ForestFire.Api.Controllers;

[ApiController]
[Route("firefighters")]
public class FirefightersController(FirefighterLocationService locationService) : ControllerBase
{
    // den seneste position for hver brandmand: GET /firefighters
    [HttpGet]
    public ActionResult<List<FirefighterLocation>> GetLocations()
    {
        return Ok(locationService.GetAll());
    }

    // mqtt-servicen sender positionerne fra firefighters/{id}/location hertil: PUT /firefighters/{id}
    [HttpPut("{id}")]
    public ActionResult UpdateLocation(string id, FirefighterLocationDto dto)
    {
        locationService.Update(new FirefighterLocation
        {
            Id = id,
            Lat = dto.Lat,
            Lon = dto.Lon,
            ReceivedAt = DateTime.UtcNow
        });
        return NoContent();
    }
}

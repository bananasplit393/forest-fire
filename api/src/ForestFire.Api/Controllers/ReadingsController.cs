using ForestFire.Api.Models;
using ForestFire.Api.Services;
using Microsoft.AspNetCore.Mvc;

namespace ForestFire.Api.Controllers;

[ApiController]
[Route("readings")]
public class ReadingsController(SensorReadingService sensorReadingService) : ControllerBase
{

    [HttpPost]
    public async Task<ActionResult<SensorReadingDto>> Create(CreateSensorReadingDto dto)
    {
        var result = await sensorReadingService.CreateReading(dto);
        return Ok(result);
    }

    // henter de nyeste målinger, nyeste først: GET /readings?limit=100
    [HttpGet]
    public async Task<ActionResult<List<SensorReadingDto>>> GetReadings([FromQuery] int limit = 100)
    {
        if (limit < 1 || limit > 1000)
            return BadRequest("limit must be between 1 and 1000.");

        var result = await sensorReadingService.GetReadings(limit);
        return Ok(result);
    }

    // henter én måling: GET /readings/{id}
    [HttpGet("{id:guid}")]
    public async Task<ActionResult<SensorReadingDto>> GetReading(Guid id)
    {
        var result = await sensorReadingService.GetReading(id);
        if (result is null) return NotFound();

        return Ok(result);
    }

    // sletter en måling: DELETE /readings/{id}
    [HttpDelete("{id:guid}")]
    public async Task<ActionResult> DeleteReading(Guid id)
    {
        var success = await sensorReadingService.DeleteReading(id);
        if (!success)
            return NotFound();

        return NoContent();
    }
}

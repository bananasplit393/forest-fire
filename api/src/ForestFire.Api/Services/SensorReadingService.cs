using ForestFire.Api.Data;
using ForestFire.Api.Models;
using Microsoft.EntityFrameworkCore;

namespace ForestFire.Api.Services;

public class SensorReadingService
{
    private readonly ForestFireDbContext db;
    private readonly RiskModelService riskModel;

    public SensorReadingService(ForestFireDbContext db, RiskModelService riskModel)
    {
        this.db = db;
        this.riskModel = riskModel;
    }

    public async Task<SensorReadingDto> CreateReading(CreateSensorReadingDto dto)
    {
        var reading = new SensorReadingModel
        {
            Id = Guid.NewGuid(),
            Timestamp = DateTimeOffset.FromUnixTimeSeconds(dto.Ts).UtcDateTime,
            Temp = dto.Temp,
            Humidity = dto.Humidity,
            Pm10 = dto.Pm10,
            Pm25 = dto.Pm25,
            Lat = dto.Lat,
            Lon = dto.Lon,
            CreatedAt = DateTime.UtcNow
        };

        reading.FireProbability = riskModel.Score(reading);
        reading.IsFire = reading.FireProbability >= 0.8; // her er det, vi kan ændre på tærskelen for beregning af brand

        db.SensorReadings.Add(reading);
        await db.SaveChangesAsync();

        return ToDto(reading);
    }

    // henter de nyeste målinger, nyeste først
    public async Task<List<SensorReadingDto>> GetReadings(int limit)
    {
        var readings = await db.SensorReadings
            .AsNoTracking()
            .OrderByDescending(r => r.Timestamp)
            .Take(limit)
            .ToListAsync();

        return readings.Select(r => ToDto(r)).ToList();
    }

    // henter en måling ift. id
    public async Task<SensorReadingDto?> GetReading(Guid id)
    {
        var reading = await db.SensorReadings
            .AsNoTracking()
            .FirstOrDefaultAsync(r => r.Id == id);

        if (reading is null) return null;
        return ToDto(reading);
    }

    // sletter målingen
    public async Task<bool> DeleteReading(Guid id)
    {
        var reading = await db.SensorReadings.FirstOrDefaultAsync(r => r.Id == id);
        if (reading is null) return false;

        db.SensorReadings.Remove(reading);
        await db.SaveChangesAsync();
        return true;
    }

    // laver målingen om til en DTO
    public static SensorReadingDto ToDto(SensorReadingModel reading)
    {
        return new SensorReadingDto
        {
            Id = reading.Id,
            Ts = new DateTimeOffset(DateTime.SpecifyKind(reading.Timestamp, DateTimeKind.Utc)).ToUnixTimeSeconds(),
            Timestamp = reading.Timestamp,
            Temp = reading.Temp,
            Humidity = reading.Humidity,
            Pm10 = reading.Pm10,
            Pm25 = reading.Pm25,
            Lat = reading.Lat,
            Lon = reading.Lon,
            FireProbability = reading.FireProbability,
            IsFire = reading.IsFire,
            CreatedAt = reading.CreatedAt
        };
    }
}


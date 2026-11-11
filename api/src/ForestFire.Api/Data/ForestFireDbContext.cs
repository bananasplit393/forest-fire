using ForestFire.Api.Models;
using Microsoft.EntityFrameworkCore;

namespace ForestFire.Api.Data;

public class ForestFireDbContext : DbContext
{
    public ForestFireDbContext(DbContextOptions<ForestFireDbContext> options) : base(options)
    {
    }

    public DbSet<SensorReadingModel> SensorReadings => Set<SensorReadingModel>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<SensorReadingModel>(entity =>
        {
            entity.HasKey(r => r.Id);

            // målingerne hentes nyeste først
            entity.HasIndex(r => r.Timestamp);
        });
    }
}

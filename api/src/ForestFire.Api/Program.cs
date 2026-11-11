using System.Text.Json.Serialization;
using ForestFire.Api.Data;
using ForestFire.Api.Services;
using Microsoft.EntityFrameworkCore;

var builder = WebApplication.CreateBuilder(args);

var connectionString = builder.Configuration.GetConnectionString("Default");

builder.Services.AddDbContext<ForestFireDbContext>(options => options.UseNpgsql(connectionString));
builder.Services.AddScoped<SensorReadingService>();

builder.Services.AddSingleton<RiskModelService>();

builder.Services.AddControllers()
    .AddJsonOptions(o => o.JsonSerializerOptions.Converters.Add(new JsonStringEnumConverter()));
builder.Services.AddOpenApi();

var app = builder.Build();

if (app.Configuration.GetValue("Database:MigrateOnStartup", false))
{
    using var scope = app.Services.CreateScope();
    var db = scope.ServiceProvider.GetRequiredService<ForestFireDbContext>();
    db.Database.Migrate();
}

if (app.Environment.IsDevelopment())
{
    app.MapOpenApi();
}


app.MapControllers();

app.Run();



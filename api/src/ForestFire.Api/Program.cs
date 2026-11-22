using ForestFire.Api.Data;
using ForestFire.Api.Interfaces;
using ForestFire.Api.Mqtt;
using ForestFire.Api.Services;
using Microsoft.EntityFrameworkCore;

var builder = WebApplication.CreateBuilder(args);

var connectionString = builder.Configuration.GetConnectionString("Default");

builder.Services.AddDbContext<ForestFireDbContext>(options => options.UseNpgsql(connectionString));
builder.Services.AddScoped<SensorReadingService>();

builder.Services.AddSingleton<RiskModelService>();

// seneste position pr. brandmand
builder.Services.AddSingleton<FirefighterLocationService>();

builder.Services.AddSingleton<MqttService>();
builder.Services.AddSingleton<IMqttPublisher>(sp => sp.GetRequiredService<MqttService>());
builder.Services.AddHostedService(sp => sp.GetRequiredService<MqttService>());
builder.Services.AddSingleton<IFireAlertSender, MqttFireAlertSender>();

builder.Services.AddControllers();

// så frontenden (anden port) må kalde API'et
builder.Services.AddCors(o => o.AddDefaultPolicy(p => p.AllowAnyOrigin().AllowAnyHeader().AllowAnyMethod()));
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

app.UseCors();
app.MapControllers();

app.Run();

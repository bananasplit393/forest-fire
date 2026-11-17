using ForestFire.Mqtt.Interfaces;
using ForestFire.Mqtt.Mqtt;
using ForestFire.Mqtt.Services;

var builder = Host.CreateApplicationBuilder(args);

// API'et (ForestFire.Api) som målinger og positioner sendes videre til. Sættes via Api__BaseUrl
var apiUrl = builder.Configuration["Api:BaseUrl"];
if (string.IsNullOrWhiteSpace(apiUrl))
    throw new InvalidOperationException("Api:BaseUrl mangler. Sæt miljøvariablen Api__BaseUrl.");

builder.Services.AddSingleton<MqttService>();
builder.Services.AddSingleton<IMqttPublisher>(sp => sp.GetRequiredService<MqttService>());
builder.Services.AddHostedService(sp => sp.GetRequiredService<MqttService>());

builder.Services.AddHttpClient<IForestFireApi, ForestFireApiClient>();
builder.Services.AddSingleton<IFireAlertSender, MqttFireAlertSender>();

var host = builder.Build();
host.Run();

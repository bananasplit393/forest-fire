using System.Text.Json;
using ForestFire.Api.Interfaces;
using ForestFire.Api.Models;
using ForestFire.Api.Services;
using MQTTnet;
using MQTTnet.Client;
using MQTTnet.Protocol;

namespace ForestFire.Api.Mqtt
{
    // samme opbygning som MqttService i Edger: én forbindelse til brokeren, der både lytter og publicerer.
    // Lytter på SensorData (sensors/+/data) og FirefighterLocation (firefighters/+/location) og gemmer direkte via services.
    // Publicerer FireAlert (alerts/{nodeId}) via IMqttPublisher, se MqttFireAlertSender.
    public class MqttService : BackgroundService, IMqttPublisher
    {
        private readonly IServiceScopeFactory scopeFactory;
        private readonly IConfiguration config;
        private readonly IMqttClient mqttClient;

        private static readonly JsonSerializerOptions jsonOptions = new(JsonSerializerOptions.Web);

        public MqttService(IServiceScopeFactory scopeFactory, IConfiguration config)
        {
            this.scopeFactory = scopeFactory;
            this.config = config;
            mqttClient = new MqttFactory().CreateMqttClient();
        }

        protected override async Task ExecuteAsync(CancellationToken stoppingToken)
        {
            var host = config["Mqtt:Host"] ?? "localhost";
            var port = config.GetValue("Mqtt:Port", 1883);
            var clientId = config["Mqtt:ClientId"] ?? "forestfire-mqtt";
            var username = config["Mqtt:Username"];
            var password = config["Mqtt:Password"];
            var sensorDataTopic = config["Mqtt:SensorDataTopic"] ?? "sensors/+/data";
            var locationTopic = config["Mqtt:FirefighterLocationTopic"] ?? "firefighters/+/location";

            var optionsBuilder = new MqttClientOptionsBuilder()
                .WithTcpServer(host, port)
                .WithClientId(clientId)
                .WithCleanSession(false);   // Mosquitto gemmer SensorData (QoS 1), hvis vi er nede

            if (!string.IsNullOrEmpty(username))
                optionsBuilder.WithCredentials(username, password);

            var options = optionsBuilder.Build();

            mqttClient.ApplicationMessageReceivedAsync += async e =>
            {
                var topic = e.ApplicationMessage.Topic;
                var json = e.ApplicationMessage.ConvertPayloadToString();

                if (TryMatch(sensorDataTopic, topic, out var nodeId))
                {
                    // ack først når målingen er gemt, så vi ikke mister målinger hvis databasen er nede
                    e.AutoAcknowledge = false;
                    var ok = await HandleSensorData(nodeId, json);
                    if (ok)
                        await e.AcknowledgeAsync(stoppingToken);
                    return;
                }

                if (TryMatch(locationTopic, topic, out var firefighterId))
                {
                    HandleFirefighterLocation(firefighterId, json);
                }
            };

            while (!stoppingToken.IsCancellationRequested)
            {
                if (!mqttClient.IsConnected)
                {
                    try
                    {
                        await mqttClient.ConnectAsync(options, stoppingToken);
                        // QoS 1: hver måling skal frem, for at vi kan opdage en brand
                        await mqttClient.SubscribeAsync(sensorDataTopic, MqttQualityOfServiceLevel.AtLeastOnce);
                        // QoS 0: positionen sendes ofte, en tabt position erstattes af den næste
                        await mqttClient.SubscribeAsync(locationTopic, MqttQualityOfServiceLevel.AtMostOnce);
                    }
                    catch
                    {
                    }
                }
                await Task.Delay(5000, stoppingToken);
            }
        }

        // SensorData {node, ts, temp, humidity, pm10, pm25, lat, lon} -> gem og scor -> FireAlert hvis isFire.
        // Giver true når beskeden er færdigbehandlet (også hvis den var ugyldig), false hvis den skal prøves igen
        private async Task<bool> HandleSensorData(string nodeId, string json)
        {
            CreateSensorReadingDto? data;
            try
            {
                data = JsonSerializer.Deserialize<CreateSensorReadingDto>(json, jsonOptions);
            }
            catch (JsonException)
            {
                return true;
            }
            if (data == null)
                return true;

            // topic'et bestemmer hvilken node målingen er fra
            data.Node = nodeId;

            try
            {
                using var scope = scopeFactory.CreateScope();
                var reading = await scope.ServiceProvider.GetRequiredService<SensorReadingService>().CreateReading(data);

                var alerts = scope.ServiceProvider.GetRequiredService<IFireAlertSender>();
                if (reading.IsFire == true)
                    await alerts.SendAsync(reading);
                else
                    await alerts.ClearAsync(nodeId);

                return true;
            }
            catch
            {
                return false;
            }
        }

        // FirefighterLocation {id, lat, lon} -> opdater position i hukommelsen
        private void HandleFirefighterLocation(string firefighterId, string json)
        {
            try
            {
                var location = JsonSerializer.Deserialize<FirefighterLocation>(json, jsonOptions);
                if (location == null)
                    return;

                location.Id = firefighterId;
                location.ReceivedAt = DateTime.UtcNow;

                using var scope = scopeFactory.CreateScope();
                scope.ServiceProvider.GetRequiredService<FirefighterLocationService>().Update(location);
            }
            catch (JsonException)
            {
            }
        }

        // matcher et topic mod et filter med ét '+', fx sensors/+/data mod sensors/node-1/data.
        // wildcard bliver det, der stod på '+'-pladsen (node-id eller brandmand-id)
        private static bool TryMatch(string filter, string topic, out string wildcard)
        {
            wildcard = string.Empty;
            var filterParts = filter.Split('/');
            var topicParts = topic.Split('/');
            if (filterParts.Length != topicParts.Length)
                return false;

            for (var i = 0; i < filterParts.Length; i++)
            {
                if (filterParts[i] == "+")
                    wildcard = topicParts[i];
                else if (filterParts[i] != topicParts[i])
                    return false;
            }
            return wildcard.Length > 0;
        }

        public async Task PublishAsync(string topic, string payload, bool retain = false, CancellationToken stoppingToken = default)
        {
            var message = new MqttApplicationMessageBuilder()
                .WithTopic(topic)
                .WithPayload(payload)
                .WithQualityOfServiceLevel(MqttQualityOfServiceLevel.AtLeastOnce)
                .WithRetainFlag(retain)
                .Build();

            await mqttClient.PublishAsync(message, stoppingToken);
        }
    }
}

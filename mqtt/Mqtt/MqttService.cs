using System.Text.Json;
using ForestFire.Mqtt.Interfaces;
using ForestFire.Mqtt.Models;
using MQTTnet;
using MQTTnet.Client;
using MQTTnet.Protocol;

namespace ForestFire.Mqtt.Mqtt
{
    // samme opbygning som MqttService i Edger: én forbindelse til brokeren, der både lytter og publicerer.
    // Lytter på SensorData (sensors/+/data) og FirefighterLocation (firefighters/+/location) og sender dem til API'et.
    // Publicerer FireAlert (alerts/{nodeId}) via IMqttPublisher, se MqttFireAlertSender.
    public class MqttService : BackgroundService, IMqttPublisher
    {
        private readonly IServiceProvider services;
        private readonly IConfiguration config;
        private readonly ILogger<MqttService> logger;
        private readonly IMqttClient mqttClient;

        private static readonly JsonSerializerOptions jsonOptions = new(JsonSerializerOptions.Web);

        public MqttService(IServiceProvider services, IConfiguration config, ILogger<MqttService> logger)
        {
            this.services = services;
            this.config = config;
            this.logger = logger;
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
                    // ack først når API'et har gemt målingen, så vi ikke mister målinger hvis API'et er nede
                    e.AutoAcknowledge = false;
                    var ok = await HandleSensorData(nodeId, json, stoppingToken);
                    if (ok)
                        await e.AcknowledgeAsync(stoppingToken);
                    return;
                }

                if (TryMatch(locationTopic, topic, out var firefighterId))
                {
                    await HandleFirefighterLocation(firefighterId, json, stoppingToken);
                    return;
                }

                logger.LogWarning("besked på ukendt topic {Topic}", topic);
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
                        logger.LogInformation("forbundet til MQTT, lytter på " + sensorDataTopic + " og " + locationTopic + " hos " + host + ":" + port);
                    }
                    catch (Exception ex)
                    {
                        logger.LogWarning(ex, "kunne ikke forbinde til MQTT");
                    }
                }
                await Task.Delay(5000, stoppingToken);
            }
        }

        // SensorData {node, ts, temp, humidity, pm10, pm25, lat, lon} -> POST /readings -> FireAlert hvis isFire.
        // Giver true når beskeden er færdigbehandlet (også hvis den var ugyldig), false hvis den skal prøves igen
        private async Task<bool> HandleSensorData(string nodeId, string json, CancellationToken ct)
        {
            logger.LogInformation("måling fra {NodeId}: {Json}", nodeId, json);

            SensorData? data;
            try
            {
                data = JsonSerializer.Deserialize<SensorData>(json, jsonOptions);
            }
            catch (JsonException ex)
            {
                logger.LogWarning(ex, "ugyldig JSON fra {NodeId}: {Json}", nodeId, json);
                return true;
            }
            if (data == null)
                return true;

            // topic'et bestemmer hvilken node målingen er fra
            data.Node = nodeId;

            try
            {
                var api = services.GetRequiredService<IForestFireApi>();
                var reading = await api.PostReadingAsync(data, ct);
                if (reading == null)
                {
                    logger.LogWarning("måling fra {NodeId} afvist af API'et", nodeId);
                    return true;
                }

                if (reading.IsFire == true)
                    await services.GetRequiredService<IFireAlertSender>().SendAsync(reading);

                return true;
            }
            catch (HttpRequestException ex)
            {
                logger.LogError(ex, "kunne ikke gemme måling fra {NodeId} i API'et, prøver igen senere", nodeId);
                return false;
            }
        }

        // FirefighterLocation {id, lat, lon} -> PUT /firefighters/{id}
        private async Task HandleFirefighterLocation(string firefighterId, string json, CancellationToken ct)
        {
            try
            {
                var location = JsonSerializer.Deserialize<FirefighterLocation>(json, jsonOptions);
                if (location == null)
                    return;

                location.Id = firefighterId;
                await services.GetRequiredService<IForestFireApi>().PutFirefighterLocationAsync(location, ct);
            }
            catch (JsonException ex)
            {
                logger.LogWarning(ex, "ugyldig position fra {FirefighterId}: {Json}", firefighterId, json);
            }
            catch (HttpRequestException ex)
            {
                // QoS 0: den næste position kommer om lidt
                logger.LogWarning(ex, "kunne ikke sende position for {FirefighterId} til API'et", firefighterId);
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

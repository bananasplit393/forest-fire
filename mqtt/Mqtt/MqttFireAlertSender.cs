using System.Text.Json;
using ForestFire.Mqtt.Interfaces;
using ForestFire.Mqtt.Models;

namespace ForestFire.Mqtt.Mqtt
{
    // bygger FireAlert-beskeden og publicerer den på alerts/{nodeId}, QoS 1 og retained,
    // så en app der forbinder senere stadig får alarmen. Beskeden er SensorData + alarm-flag.
    public class MqttFireAlertSender : IFireAlertSender
    {
        private readonly IMqttPublisher mqtt;
        private readonly ILogger<MqttFireAlertSender> logger;
        private readonly string Topic;
        private static readonly JsonSerializerOptions jsonOptions = new(JsonSerializerOptions.Web);

        public MqttFireAlertSender(IMqttPublisher mqtt, IConfiguration config, ILogger<MqttFireAlertSender> logger)
        {
            this.mqtt = mqtt;
            this.logger = logger;
            Topic = config["Mqtt:FireAlertTopic"] ?? "alerts/{0}";
        }

        public async Task SendAsync(Reading reading)
        {
            if (string.IsNullOrWhiteSpace(reading.Node))
            {
                logger.LogWarning("måling {Id} har ingen node, kan ikke sende alarm", reading.Id);
                return;
            }

            var topic = string.Format(Topic, reading.Node);

            var message = new
            {
                alertId = reading.Id,   // dubletter (QoS 1) kan kendes på id'et
                node = reading.Node,
                ts = reading.Ts,
                temp = reading.Temp,
                humidity = reading.Humidity,
                pm10 = reading.Pm10,
                pm25 = reading.Pm25,
                lat = reading.Lat,
                lon = reading.Lon,
                fireProbability = reading.FireProbability,
                alarm = true
            };

            var json = JsonSerializer.Serialize(message, jsonOptions);

            try
            {
                await mqtt.PublishAsync(topic, json, retain: true);
                logger.LogInformation("FireAlert sendt på {Topic}: {Json}", topic, json);
            }
            catch (Exception ex)
            {
                // målingen er allerede gemt i API'et, så vi logger bare fejlen
                logger.LogError(ex, "kunne ikke sende FireAlert på {Topic}", topic);
            }
        }
    }
}
